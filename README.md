# Полезные Ссылки!
- [elLauncher.bat - установщик (поместить в папку)](https://github.com/Niclic2/elLauncher/releases/download/elLauncher-0.0.2/elLauncher.bat)  
- [elLauncher.zip - Архив с уже скаченным лаунчером](https://github.com/Niclic2/elLauncher/releases/download/elLauncher-0.0.2/elLauncher.zip)

# Проблемы, которые были при создании лаунчера
- Отсутствие звука, языков, иконки программы. Является отсутсвием ассетов или путь к ним неверный.  
Путь к ассету должен выглядеть так "assets\objects\00\000c82756fd54e40cb236199f2b479629d0aca2f"  
- Ебучие Json'ы маджонга зебали меня и я решил их полностью переделать под elLauncher.  
Из-за этого пришлось переделать половину кода под новые json'ы, но это более надежно и гибче.  
- После обновления windows была ошибка от tasklist: Недопустимый класс. Нашел ответ на форуме.
Открывем командную строку cmd от имени администратора и вписываем по очереди:  
_**cd \windows\system32\wbem**_ | _**net stop winmgmt**_ | _**rename Repository Repository.old**_ | _**net start winmgmt**_
