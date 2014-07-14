package ru.shubert.insta.logic

object StrCons {
  val SCAN = "сканируем"
  val PAUSE = "отдыхаем %dс"
  val REQUEST = "опрашиваем сервер"
  val RETROSPECTION = "смотрим в прошлое"
  val DOWNLOAD = "скачиваем фото"
  val STOPPED = "остановлен"
  val ERROR = "ошибка %s"
  val NET_ERROR = "кажется, нет сети."
  final val STARTBTN = Map(false -> "Остановить", true -> "Сканировать")
  val LOGO_IS_EMPTY = "логотип не выбран"
  val LOGO = "логотип"
  val ZERO = "0"
  val ZOOM = "<html>Для масштабирования: <br />ЛКМ на фото и '+'/'-'<br /> или ctrl+колесо мыши</html>"

  val ILINK = "http://instagram.com/developer/authentication/#"
  final val POSITIONS = Array[String]("Налево", "В центр", "Направо")

  val TAB1 = "Загрузки"
  val TAB2 = "Настройки"
  val SECRET: String = "Client secret"
  val TOKEN: String = "Token"
  val ACTION: String = "Действие"
  final val ACTIONS = Array("Печатать", "Сохранить", "Печатать и сохранить")
  val DURATION: String = "Захват, мин."
  val STATUS = "Статус:"
  val DOWNLOADED = "Приняли:"
  val SEEN = "Просмотрено:"
  val TAG = "#хэштаг"
  val SLOGAN = "cлоган"

  val ERROR_TAG = "таг пуст или содержит спецсимволы/пробелы!"
  val WARN_TAG = "из тага удалены пробелы/спецсимволы"
  val ERROR_LOAD_LOGO = "Не удается загрузить лого %s"

  val STUB_TAG = "batman"
  val STUB_SLOGAN = "A very special and cool party slogan"
  val STUB_USER = "Daniel"
  val STUB_SECRET = ""
  val STUB_TOKEN = ""
}