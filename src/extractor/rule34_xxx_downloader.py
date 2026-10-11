import downloader
import ree as re
import os
from utils import Downloader, query_url, Soup, get_max_range, get_print, clean_title, try_n, check_alive, clean_url, File, urljoin
from translator import tr_
from urllib.parse import quote
import clf2
import utils


def get_tags(url):
    url = clean_url(url)
    qs = query_url(url)
    if 'page=favorites' in url:
        id = qs.get('id', ['N/A'])[0]
        id = 'fav_{}'.format(id)
    else:
        tags = qs.get('tags', [])
        tags.sort()
        id = ' '.join(tags)
    if not id:
        id = 'N/A'
    return id



class Downloader_rule34_xxx(Downloader):
    type = 'rule34_xxx'
    URLS = ['rule34.xxx']
    MAX_CORE = 8
    display_name = 'Rule34.xxx'
    _name = None
    ACCEPT_COOKIES = [r'(.*\.)?rule34\.xxx']

    @classmethod
    def fix_url(cls, url):
        if 'rule34.xxx' not in url.lower():
            url = url.replace(' ', '+')
            while '++' in url:
                url = url.replace('++', '+')
            url = quote(url)
            url = url.replace('%2B', '+')
            url = 'https://rule34.xxx/index.php?page=post&s=list&tags={}'.format(url)
        return url.replace('http://', 'https://')

    @property
    def name(self):
        if self._name is None:
            tags = get_tags(self.url)
            self._name = tags
        return clean_title(self._name)

    def read(self):
        self.title = self.name

        self.urls += get_imgs(self.url, self.name, cw=self.cw)

        self.title = self.name


class Image(File):
    type = 'rule34_xxx'
    format = 'id'
    def get(self):
        soup = read_soup(self['referer'], self.cw)
        url = soup.find('meta', {'property': 'og:image'})['content']
        ext = utils.get_ext(url)
        d = {
            'id': self['id'],
            }
        if getattr(self.cw, '?rule34_xxx_clf2', False):
            url = 'tmp://' + clf2.download(url, cw=self.cw)
        return {'url': url, 'name': utils.format('rule34_xxx', d, ext)}


def setPage(url, page):
    # Always use HTTPS
    url = url.replace('http://', 'https://')

    # Change the page
    if 'pid=' in url:
        url = re.sub('pid=[0-9]*', 'pid={}'.format(page), url)
    else:
        url += '&pid={}'.format(page)

    return url


def get_imgs(url, title=None, cw=None):
    url = clean_url(url)
    if 's=view' in url and 'page=favorites' not in url:
        raise NotImplementedError('Not Implemented')

    if 'page=post' not in url.lower():
        tags = get_tags(url)
        tags = quote(tags, safe='/')
        tags = tags.replace('%20', '+')
        url = "https://rule34.xxx/index.php?page=post&s=list&tags={}&pid={}".format(tags, 0) #8382

    print_ = get_print(cw)

    # Range
    max_pid = get_max_range(cw)

    imgs = []
    ids = set()
    for p in range(500): #1017
        check_alive(cw)
        url = setPage(url, len(imgs))
        print_(url)

        soup = read_soup(url, cw)
        posts = soup.findAll('span', class_='thumb')
        if not posts:
            break
        for post in posts:
            id_ = post.attrs['id']
            if id_ in ids:
                print('duplicate:', id_)
                continue
            ids.add(id_)
            img = Image({'referer':urljoin(url, post.find('a')['href']), 'id':id_})
            imgs.append(img)
        if len(imgs) >= max_pid:
            break

        if cw is not None:
            cw.setTitle('{}  {} - {}'.format(tr_('읽는 중...'), title, len(imgs)))
    return imgs


@try_n(4, sleep=30)
def read_soup(url, cw): #8382
    try:
        return downloader.read_soup(url) #3340
    except:
        setattr(cw, '?rule34_xxx_clf2', True)
        return Soup(clf2.solve(url, cw=cw)['html'])
