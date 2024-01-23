## VKontakte group statistics parser

![Java](https://img.shields.io/badge/-Java-05122A?style=flat&logo=Java&logoColor=FFA518) ![WebService](https://img.shields.io/badge/-WebService-05122A?style=flat) ![Spring](https://img.shields.io/badge/-Spring-05122A?style=flat&logo=Spring&logoColor=71b23c) ![Springboot](https://img.shields.io/badge/-SpringBoot-05122A?style=flat&logo=Springboot&logoColor=71b23c) ![MySQL](https://img.shields.io/badge/-MySQL-05122A?style=flat&logo=MySQL&logoColor=fffffb) ![Maven](https://img.shields.io/badge/-Maven-05122A?style=flat&logo=apachemaven&logoColor=fffffb) ![VK SDK](https://img.shields.io/badge/-VK_SDK-05122A?style=flat&logo=vk) ![Thymeleaf](https://img.shields.io/badge/-Thymeleaf-05122A?style=flat&logo=Thymeleaf) ![REST](https://img.shields.io/badge/-REST-05122A?style=flat)

Java RESTful application for parsing statistics data from VK, saving them in the database,
processing and obtaining data for the required period.


Technologies:
* Java 11
* MySQL 8
* SpringBoot (starters: web, data-jpa, thymeleaf)
* VK SDK
* Maven
* Thymeleaf

In a separate daemon thread, it periodically receives the following data via the VK API for each group:
 * Name,
 * number of likes,
 * number of comments,
 * number of participants,
 * number of views,
 * number of posts
 
 After receiving it, enters the information into the database.
 Provides access to received data via REST API.
 
 When accessing the application
  to addresses `/stats/today`, `/stats/week`, `/stats/month`, `/stats/year`
 The database is contacted and the requested statistics are obtained
 for the corresponding period.
 
 If the application is not authorized and cannot update data in the database
 data, then a redirection occurs to the VK website for authorization and receipt of a token.
 
 * Program settings are moved to [application.yml](application.yml)
 * The list of groups (id) is specified in [VkClient](src\main\java\ru\list\surkovr\VkClient.java)
 
 VK SDK has [unfixed bug](https://github.com/VKCOM/vk-java-sdk/issues/178) when deserializing objects with URL links,
 Accordingly, it may not work correctly.
 Ready for deployment to Heroku.