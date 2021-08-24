## Парсер статистики групп ВКонтакте

![Java](https://img.shields.io/badge/-Java-05122A?style=flat&logo=Java&logoColor=FFA518) ![WebService](https://img.shields.io/badge/-WebService-05122A?style=flat) ![Spring](https://img.shields.io/badge/-Spring-05122A?style=flat&logo=Spring&logoColor=71b23c) ![Springboot](https://img.shields.io/badge/-SpringBoot-05122A?style=flat&logo=Springboot&logoColor=71b23c) ![MySQL](https://img.shields.io/badge/-MySQL-05122A?style=flat&logo=MySQL&logoColor=fffffb) ![Maven](https://img.shields.io/badge/-Maven-05122A?style=flat&logo=apachemaven&logoColor=fffffb) ![VK SDK](https://img.shields.io/badge/-VK_SDK-05122A?style=flat&logo=vk) ![Thymeleaf](https://img.shields.io/badge/-Thymeleaf-05122A?style=flat&logo=Thymeleaf) ![REST](https://img.shields.io/badge/-REST-05122A?style=flat)

Java RESTful приложение для парсинга данных статистики с ВК, сохранения их в БД, 
обработки и получения данных за нужный период.



Технологии:
* Java 11
* MySQL 8
* SpringBoot (starters: web, data-jpa, thymeleaf)
* VK SDK
* Maven
* Thymeleaf

В отдельном демон-потоке периодически получает следующие данные по VK API для каждой группы:
 * название, 
 * количество лайков, 
 * количество комментариев, 
 * количество участников, 
 * количество просмотров,
 * количество постов
 
 После получения заносит информацию в БД.
 Предоставляет доступ к полученным данным по REST API. 
 
 При обращение в приложение
  на адреса `/stats/today`, `/stats/week`, `/stats/month`, `/stats/year` 
 Происходит обращение в базу данных и получение запрошенной статистики 
 для соответствующего периода. 
 
 Если при этом приложение не авторизовано и не может обновлять данные в базе 
 данных, то происходит переадресация на сайт ВК для авторизации и получения токена.
 
 * Настройки программы вынесены в [application.yml](application.yml)
 * Список групп (id) задается в [VkClient](src\main\java\ru\list\surkovr\VkClient.java)
 
 В VK SDK имеется [неустраненный баг](https://github.com/VKCOM/vk-java-sdk/issues/178) при десериализации объектов с ссылками URL, 
 соответственно может работать некорректно.
 Готово для деплоя в Heroku.