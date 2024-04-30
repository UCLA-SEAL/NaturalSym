import bs4
import requests
import pandas as pd
from datetime import datetime
totalItems = 5976
def numeric_value(movie, tag, class_=None, order=None):
    if order:
        if len(movie.findAll(tag, class_)) > 1:
            to_extract = movie.findAll(tag, class_)[order]['data-value']
        else:
            to_extract = None
    else:
        to_extract = movie.find(tag, class_)['data-value']
 
    return to_extract
 
 
def text_value(movie, tag, class_=None):
    if movie.find(tag, class_):
        return movie.find(tag, class_).text.replace("\n", "")
    else:
        return
 
 
def nested_text_value(movie, tag_1, class_1, tag_2, class_2, order=None):
    if not order:
        if movie.find(tag_1, class_1).find(tag_2, class_2)!=None:
            return movie.find(tag_1, class_1).find(tag_2, class_2).text
        else:
            return 'NA' 
    else:
        return [val.text for val in movie.find(tag_1, class_1).findAll(tag_2, class_2)[order]]
 
 
def extract_attribute(soup, tag_1, class_1='', tag_2='', class_2='',
                      text_attribute=True, order=None, nested=False):
    movies = soup.findAll('div', class_='lister-item-content')
    data_list = []
    for movie in movies:
        if text_attribute:
            if nested:
                data_list.append(nested_text_value(movie, tag_1, class_1, tag_2, class_2, order))
            else:
                data_list.append(text_value(movie, tag_1, class_1))
        else:
            data_list.append(numeric_value(movie, tag_1, class_1, order))
 
    return data_list
def extract_poster(soup):
    data_list = []
    movies = soup.findAll('div', class_='lister-item-image')
    data_list = []
    for movie in movies:
        x= movie.find('a').img['loadlate']
        if "@." in x:
          url = x.rsplit('@.')[0]+'@.jpg'
        else:
          url = ".".join(x.split('.')[0:-2])+'.jpg'
          if url == "https://m.media-amazon.jpg":
            url=""
        data_list.append(url)
    return data_list
def extract_lqposter(soup):
    data_list = []
    movies = soup.findAll('div', class_='lister-item-image')
    data_list = []
    for movie in movies:
        url= movie.find('a').img['loadlate']
        data_list.append(url)
    return data_list
def extract_imdbID(soup):
    data_list = []
    movies = soup.findAll('div', class_='lister-top-right')
    data_list = []
    for movie in movies:
        data_list.append(movie.div['data-tconst'])
    return data_list
def get_page_contents(url):
    page = requests.get(url,headers={"Accept-Language": "en-US"})
    return bs4.BeautifulSoup(page.text, "html.parser")
 
def extract_LINK(soup):
    movies = soup.findAll('h3', class_='lister-item-header')
    data_list = []
    for movie in movies:
        data_list.append('https://www.imdb.com'+movie.a['href'])
    return data_list
def extract_plot(link):
    soup = get_page_contents(link)
    plot=''
    if soup.find('span',class_='sc-16ede01-0')!=None:
      plot = soup.find('span',class_='sc-16ede01-0').text
    return plot
i=0
url='https://www.imdb.com/search/title/?title_type=feature,tv_movie&ref_=adv_prv'
while i<=totalItems:
    print(i)
    soup = get_page_contents(url)
    titles = extract_attribute(soup, 'a')
    release = extract_attribute(soup, 'span', 'lister-item-year text-muted unbold')
    audience_rating = extract_attribute(soup, 'span', 'certificate')
    runtime = extract_attribute(soup, 'span', 'runtime')
    genres = extract_attribute(soup, 'span', 'genre')
    imdb_rating = extract_attribute(soup, 'div', 'inline-block ratings-imdb-rating', False)
    ivotes=[]
    votes = extract_attribute(soup, 'span' , {'name' : 'nv'}, False, 0)
    for v in votes:
      if v !=None:
        ivotes.append(v.replace(',', ''))
      else:
        ivotes.append(0)
    posters = extract_poster(soup)
    print(posters)
    lq_posters= extract_lqposter(soup)
    imdbIDs = extract_imdbID(soup)
    links = extract_LINK(soup)
    plots=[]
    for l in links:
      plot = extract_plot(l)
      plot = plot.replace('\n',"")
      plots.append(plot)
    iyears = extract_attribute(soup, 'span', 'lister-item-year')
    years=[]
    for year in iyears:
      years.append("".join(c for c in year if  c.isdecimal()))
    movies = soup.findAll('div',class_='lister-item-content')
    directors=[]
    actors=[]
    for movie in movies:
      dirlist = []
      actlist = []
      crewblock = movie.findAll('p')
      crewtags = crewblock[2].findAll()
      foundspan = False
      for tag in crewtags:
        if tag.name == 'a' and not foundspan:
          dirlist.append(tag.text)
        if tag.name == 'a' and foundspan:
          actlist.append(tag.text)
        if 'span' in tag.name:
          foundspan = True
      directors.append(",".join(dirlist))
      actors.append(",".join(actlist))
    df_dict = {
               '_id':imdbIDs,
               'title': titles,
               'year':years,
               'genres': genres,
               'link':links,
               'audience_rating': audience_rating,
               'runtime': runtime,
               'rating': imdb_rating,
               'votes': ivotes,
               'plot':plots,
               'lq_poster':lq_posters,
               'poster':posters,
               'actors':actors,
               'directors':directors
               }
    df = pd.DataFrame(df_dict)
    with open('movies.txt', 'a') as csv_file:
      df.to_csv(path_or_buf=csv_file,index=False,header=False)
    i=i+50
    print(url+"Completed\n")
    print(url+"Started\n")
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S")
    print("Current Time =", current_time)
    soup = get_page_contents(url)
    url =soup.find('a',class_="lister-page-next next-page").get('href')
    url = 'https://www.imdb.com'+url
    print(url)
else:
    print('Completed')