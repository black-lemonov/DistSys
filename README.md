### Как компилировать
```sh
javac -cp <путь к mpj.jar> <файл java>
```
### Как запускать
```sh
mpjrun.sh -np <число потоков> <класс>
```
### Пример:
```sh
javac -cp ../lib/mpj.jar Lab3.java
mpjrun.sh -np 7 Lab3
```
