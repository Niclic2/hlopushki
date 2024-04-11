# Полезные Ссылки
- [elLauncher.zip - Архив с уже скаченным лаунчером (!Рекомендуется!)](https://github.com/Niclic2/elLauncher/releases/download/elLauncher-0.0.2/elLauncher.zip)
- [elLauncher.bat - установщик (поместить в папку)](https://github.com/Niclic2/elLauncher/releases/download/elLauncher-0.0.2/elLauncher.bat)
- [elLauncher.zip - Архив с Google диска](https://drive.usercontent.google.com/download?id=12npoVV1gq1wvhWkzZMY1S414k_RmAu-k&export=download&authuser=0&confirm=t&uuid=26226b1d-53c5-4673-9a67-7a1ff7b806ff&at=APZUnTWaUZRlVoABqWTgZMy0Fpt8:1712846515228)
# Что делать...
### Можно
- Удалять файлы (кроме установочника), все равно они скачаются заново.
### Не рекомендуется
- Открывать от имени администратора.
- Помещать в путь с кириллицей.
- Переименовывать файлы. Бессмысленно, лаунчер скачает файлы, которые ты переименовал.
# Проблемы, которые были при создании лаунчера
- Отсутствие звука, языков, иконки программы. Является отсутсвием ассетов или путь к ним неверный.  
Путь к ассету должен выглядеть так "assets\objects\00\000c82756fd54e40cb236199f2b479629d0aca2f"  
- Ебучие Json'ы маджонга зебали меня и я решил их полностью переделать под elLauncher.  
Из-за этого пришлось переделать половину кода под новые json'ы, но это более надежно и гибче.  
- После обновления windows была ошибка от tasklist: Недопустимый класс. Нашел ответ на форуме.
Открывем командную строку cmd от имени администратора и вписываем по очереди:  
_**cd \windows\system32\wbem**_ | _**net stop winmgmt**_ | _**rename Repository Repository.old**_ | _**net start winmgmt**_  
- Многие разархиваторы недолюбливают rar, поэтому используются zip архивы.
