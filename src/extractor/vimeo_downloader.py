import downloader
from io import BytesIO as IO
from utils import Downloader, LazyUrl, get_ext, format_filename, try_n, get_print, uuid, print_error
import ytdl
from m3u8_tools import M3u8_stream
import threading
import putils
import os
import ffmpeg



class Downloader_vimeo(Downloader):
    type = 'vimeo'
    URLS = ['vimeo.com']
    single = True
    ACCEPT_COOKIES = [r'(.*\.)?vimeo\.com']

    def init(self):
        if 'vimeo.com' not in self.url.lower():
            self.url = 'https://vimeo.com/{}'.format(self.url)

    def read(self):
        video = Video(self.url, cw=self.cw)
        video.url()#

        self.urls.append(video.url)
        self.setIcon(video.thumb)

        self.enableSegment()

        self.title = video.title
        self.artist = video.artist


def format_(f):
    if f is None:
        return 'None'
    return 'format:{} - resolution:{} - vbr:{} - abr:{} - url:{}'.format(f['format'], f['resolution'], f['vbr'], f['abr'], f['url'])


class Video:
    _url = None
    audio = None

    def __init__(self, url, cw=None):
        self.url = LazyUrl(url, self.get, self, pp=self.pp)
        self.cw = cw

    @try_n(4)
    def get(self,  url):
        if self._url:
            return self._url
        cw = self.cw
        print_ = get_print(cw)

        ydl = ytdl.YoutubeDL(cw=cw)
        info = ydl.extract_info(url)
        for f in info['formats']:
            print_(format_(f))
        fs = [f for f in info['formats'] if int(f['vbr'])]
        fs = sorted(fs, key=lambda f: int(f.get('width') or 0), reverse=True)
        fs_audio = [f for f in info['formats'] if not f['vbr']]
        if not fs:
            raise Exception('No MP4 videos')
        if not fs_audio:
            print_('No audio')
        f = fs[0]

        self.thumb = IO()
        try: #7780
            self.thumb_url = info['thumbnails'][0]['url']
        except:
            self.thumb_url = None
        if self.thumb_url:
            downloader.download(self.thumb_url, buffer=self.thumb)
        self.title = info['title']
        if f['protocol'] in ['http', 'https']:
            url_video = f['url']
        else:
            url_video = M3u8_stream(f['url'])
        if int(f['abr']) == 0 and fs_audio:
            if f['protocol'] not in ['http', 'https']:
                url_video.reencode = False
            self.audio = fs_audio[-1]
            def f(audio):
                try:
                    print_(f'Download audio: {format_(audio)}')
                    path = os.path.join(putils.DIRf, f'{uuid()}_a.tmp')
                    if cw is not None:
                        cw.trash_can.append(path)
                    if audio['protocol'] in ['http', 'https']:
                        url_audio = audio['url']
                    else:
                        url_audio = M3u8_stream(audio['url'])
                        url_audio.reencode = False
                    downloader.download(url_audio, outdir=os.path.dirname(path), fileName=os.path.basename(path), customWidget=cw, overwrite=True)
                    self.audio_path = path
                except Exception as e:
                    print_(print_error(e))
            self.thread_audio = threading.Thread(target=f, args=(self.audio,), daemon=True)
            self.thread_audio.start()
        ext = '.mp4'
        self.artist = info['uploader']
        self.filename = format_filename(self.title, info['id'], ext, artist=self.artist) #7127
        self._url = url_video
        return self._url

    def pp(self, filename):
        if self.audio is None:
            return filename
        self.thread_audio.join()
        ext, out = ffmpeg.merge(filename, self.audio_path, cw=self.cw)
        return filename
