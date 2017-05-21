package common

/**
 * https://foratable.com/bookapi2/dem
 * Book-API version:1.2
 * lunchgate = foratable
 * 
 * Workflow Reservationprozess
 * 
 * Situation
 * Mit der Api lässt sich komplett einen Reservationsprozess implementieren d.h damit lässt 
 * sich ein eigenes Reservationsfenster programmieren.
 * 
 * 1. Initialisieren
 * Im ersten Schritt initialisiert man den Reservationsprozess mit der Methode initReservation. 
 * Die Rückgabe beiinhaltet den request_hash, der für alle weiter API-Calls dieser Reservation 
 * benötigt wird.
 * Des weiteren beinhaltet die Rückgabe Information zum Restaurant(Name, Adresse etc) sowie die 
 * Essenszeiten der ganzen Woche.
 * 
 * 2. Tisch
 * Sobald der Gast Datum, Zeit und Anzahl Personen bestimmt hat muss die Methode setTable aufgerufen 
 * werden um zu überprüfen ob zu diesem Zeitpunkt überhaupt eine Reservation möglich ist.
 * Im Erfolgsfall kommt in der Antwort neben status=ok auch choice=[active|inactive]. Ist choice 
 * active sollte als nächstes das Auswahlfenster angezeigt werden.
 * 
 * 3. Auswahl / Kommentar
 * In der Antwort der Intialisierung wurde ist bestimmt on des Restaurant ein Kommentar erlaubt 
 * und ob es eine Auswahl gibt. Dieser Schriitt muss nur gemacht werden wenn ein Kommentarfenster 
 * angeboten wird oder die ausgewählte Zeit/Datum sich in der Auswahl Zeit/Datum befindet. 
 * Mit der Methode setChoice kann die Auswahl und der Kommentar gesetzt werden.
 * 
 * 4. Gast
 * Danach gibt der Gast seine Angaben(Name/Vorname/e-Mail/Mobile)an. Mit der Methode 
 * setGuest werden diese Daten der API übermittelt. Ist der Gast schon bei forAtable 
 * registriert wird die Reservation automatisch ausgelöst.
 * Ist der Gast unbekannt oder er ist schon mit einer anderen Mobile Nr oder e-Mail 
 * registriert wird dem Gast eine SMS mit einem viersteligen Code gesendt.
 * 
 * 5. Code überprüfen
 * Der per SMS erhaltene Code muss von Gast eingeben werden und dann mit der Method verifyCode 
 * der API übermittelt werden. Ist der Code korrekt wird die Reservation ausgelöst.
 * 
 * 
 * Workflow Zeit, Datum und Anzahl Personen am Reservationsfenster übermittlen
 * 
 * Situation
 * Datum, Zeit und Anzahl Personen werden auf der Seite des Gatronnomen erfasst.
 * 
 * Foratable stellt im Moment dafür kein Widget bereit. Dieses muss also vom Gastrom slebst programmiert werden.
 * Das Reservationsfenster wird dann per POST aufgerufen(mitgegeben wird Datum, 
 * Zein und Personenanzahl) und der Gast landet im Reservationsfenster in der entsprechenden 
 * Stelle wo er dann den Reservationsprozess fortsetzen kann.
 * 
 * 1. Restaurant Information
 * Mit der Api-Methode getRestaurantInfo. lassen sich die Restaurant relevanten Information beziehen.
 * 
 * 2. Ins Reservations springen
 * Die Daten müssen per POST dem Reservationsfenster übermittelt werden. Das sollte in einem neuen 
 * Fenster geschehenn. Der Gast befindet sich danch im Reservationsfenster, also auf foratable.com.
 * Unter der Pseudo Api-Methode Direct ist das beschrieben und kann dort getestet werden.
 * 
 */
class Booking {
  
  val baseURL = "https://foratable.com//bookapi2/"

  /**
   * Initialisiert den Reservationsprozess und gibt bei Erfolg den Request-Hash zurück, 
   * welcher beim weitern Verlauf des Reservationsprozess benötigt wird.  Des weiteren 
   * werden Daten zum Restaurant zurückgeben geben und sowie die Zeiten für welche einen 
   * Tisch reserviert werden kann am heutigen Tag
   */
  def initReservation(restaurant_hash: String, username: String, pswd: String) = {
    val url = baseURL + "initReservation"
  }
  
  /**
   * Mit dieser Methode werden die Daten(Datum, Zeit,Anzahl Personen) übergeben. 
   * Es wird geprüft ob zu diesem Zeitpunkt für die Anzahl Personen überhaupt 
   * reserviert werden kann kann.
   * 
   * Zürückgeben wird die Eigenschaft choice welche den Wert active oder inactive hat. 
   * Das darauffolgende Auswahlfenster soll nur bei active angezeigt werden
   * 
   * date YYYY-MM-DD zb. 2012-09-13
   * time HH:ii z.b 12:30
   */
  def setTable(request_hash: String, date: String, time: String, person: String) = {
    val url = baseURL + "setTable"
  }

  /**
   * Mit dieser Methode lässt sich die Auswahl setzen. 
   * Diese Methode muss nach der Methode setTable aufgerufen werden
   */
  def setChoice(request_hash: String, choice: String) = {
    val url = baseURL + "setChoice"
  }
  
  /**
   * Die Daten des Gastes werden geprüft. Ist der Gast schon registriert 
   * im System wird die Reservation aus gelöst. Ist der Gast unbekannt oder 
   * die E-Mail und die Moobile Nummer stimmen nicht überein wird dem Gast ein 
   * SMS mit einem Verifizierungs Code geschickt.
   */
  def setGuest(request_hash: String, firstname: String, lastname: String, mobile: String, email: String, countryCode: String) = {
    val url = baseURL + "setGuest"
  }

  /**
   * Der Code welcher der Gast per SMS bekommen hat wird auf Übereinstimmung geprüft. 
   * Ist der Code richtig wird die Reservation ausgelöst.
   */
  def verifyCode(request_hash: String, code: String) = {
    val url = baseURL + "verifyCode"
  }
  
  /**
   * Mit dieser Methode kann ein weiteres SMS mit dem Verfizierungs-Code an den Gast versendet werden. 
   * Da es vorkommen kann, dass eine SMS nicht ankommt, kann man dem Gast optional 
   * anbieten einen neuen Code anzufordern.
   */
  def newCode(request_hash: String) = {
    val url = baseURL + "newCode"
  }
  
  /**
   * Mit dieser Methode kann man die Sprache der Reservation setzen. Die Meldungen 
   * der Api sind dann in der gesetzten Sprache und die Bestätigung(SMS und/oder E-Mail)
   */
  def setLanguage(request_hash: String) = {
    val url = baseURL + "setLanguage"
  }
  
  /**
   * Nachdem der Gast in einem Restaurant gegessen hat bekommt er ein E-Mail mit einem 
   * Link um das Feedback abzugeben. Mit dieser Methode kann ein Feedback abgegeben 
   * werden zu einer bestimmten Reservation.
   */
  def setFeedback(request_hash: String) = {
    val url = baseURL + "setFeedback"
  }
  
  /**
   * Mit dieser Methode kann man den forAtable Restaurant-Hash abholen. 
   * Es wird eine komma-separierte Liste von Luchgate Ids mitgepostet. Die Lunchgate Ids 
   * erhält man von der Lunchgate API
   */
  def getRestaurantHash(lunchgate_ids: String, username: String, pswd: String) = {
    val url = baseURL + "getRestaurantHash"
  }

  /**
   * Es werden Informationen zum Restaurant zurückgegeben welche unter Umständen benötigt werden 
   * um ein Kalender-Widget zu programmieren.
   * 
   * In der Properts times sind die Servicezeiten der entsprechenden Tage aufgelistet. 
   * Der Index 0 ist der Sonntag und index & der Samstag
   * 
   * Im Object restaurant hat eine Property namens closed_days welches in array mit den 
   * geschlossen Wochentagen enthält. Der Wert 1 ist der Montag, der Wert 7 ist der Sonntag, 
   * der Wert 0 bedeutet, dass das Restaurant in jedem Wochentag offen ist.
   * 
   * Für genaure Angaben zum Datensatz beim Support nachfragen
   */
  def getRestaurantInfo(restaurant_hash: String, username: String, pswd: String) = {
    val url = baseURL + "getRestaurantInfo"
  }

  /**
   * Das Reservationsfenster wird per POST aufgerufen(mitgegeben wird Datum, Uhrzeit und Personenanzahl).
   * 
   * Der Gast wird dann automatisch im Reservationsfenster an die Stelle geleitet 
   * wo er die Reservation fortsetzen kann.
   * 
   * Das ist kein Api-Methode sondern einen Browser-Request.
   */
  def direct(restaurant_hash: String, date: String, time: String, places: String) = {
    val url = "https://foratable.com/reserve/restaurant/" + restaurant_hash + "/direct"
  }
  
  /**
   * Mit dieser Funktion können die verfügbaren Kontingente der verschiedenen Restaurant 
   * zu einem bestimmten Zeitpunkt(Datum/Uhrzeit) abgerufen werden.
   */
  def getContingents(date: String, username: String, pswd: String) = {
    val url = baseURL + "getContingents"
  }
}