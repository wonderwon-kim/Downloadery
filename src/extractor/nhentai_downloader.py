import downloader
import ree as re
from utils import urljoin, File, Downloader, try_n, join, get_ext, get_print
import utils
import clf2


def get_id(url):
    try:
        return int(url)
    except:
        return int(re.find('/g/([0-9]+)', url))


class File_nhentai(File):
    type = 'nhentai'
    format = 'page:04;'


class Downloader_nhentai(Downloader):
    type = 'nhentai'
    URLS = ['nhentai.net']
    MAX_CORE = 16
    display_name = 'nhentai'
    ACCEPT_COOKIES = [r'(.*\.)?nhentai\.net']

    def init(self):
        res = clf2.solve(self.url, cw=self.cw)
        self.session = res['session'] #4541

    @classmethod
    def fix_url(cls, url):
        return f'https://nhentai.net/g/{get_id(url)}/'

    def read(self):
        info, imgs = get_imgs(get_id(self.url), self.session, self.cw)

        # 1225
        artist = join(info['artists'])
        self.artist = artist if info['artists'] else None
        group = join(info['groups'])
        lang = info['lang'] or 'N／A'
        series = info['seriess'][0] if info['seriess'] else 'N／A'
        title = self.format_title(info['type'], info['id'], info['title'], artist, group, series, lang)

        self.urls += imgs

        self.title = title


@try_n(4)
def get_imgs(id, session, cw):
    print_ = get_print(cw)
    url = f'https://nhentai.net/g/{id}/1/'
    referer = f'https://nhentai.net/g/{id}/'
    info = {}

    gal = downloader.read_json(f'https://nhentai.net/api/v2/galleries/{id}', referer=referer)

    info['id'] = int(gal['id'])
    id_media = int(gal['media_id'])
    info['title'] = gal['title']['english']
    artists = []
    groups = []
    seriess = []
    for tag in gal['tags']:
        type = tag['type']
        if type == 'artist':
            artists.append(tag['name'])
        elif type == 'group':
            groups.append(tag['name'])
        elif type == 'parody' and tag['name'] != 'original':
            seriess.append(tag['name'])
        elif type == 'language':
            lang = tag['name']
        elif type == 'category':
            type_ = tag['name']
    info['artists'] = artists
    info['groups'] = groups
    info['seriess'] = seriess
    info['lang'] = lang
    info['type'] = type_
    info['lang'] = lang

    imgs = []
    for p, page in enumerate(gal['pages']):
        p += 1
        url_page = f'https://nhentai.net/g/{id}/{p}/'
        url_img = urljoin('https://i2.nhentai.net', page['path'])
        ext = get_ext(url_img)
        d = {
            'page': p,
            }
        img = File_nhentai({'url': url_img, 'referer': url_page, 'name': utils.format('nhentai', d, ext)})
        imgs.append(img)

    return info, imgs
