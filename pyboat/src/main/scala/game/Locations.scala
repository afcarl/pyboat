package pyboat.game

object AllowedLocations {
  val allowedLocations = Set(
    "Edinburgh",
    "Liverpool",
    "London",
    "Marseilles",
    "Paris",
    "Brest",
    "Venice",
    "Rome",
    "Naples",
    "Munich",
    "Berlin",
    "Kiel",
    "Vienna",
    "Trieste",
    "Budapest",
    "Constantinople",
    "Ankara",
    "Smyrna",
    "Moscow",
    "St. Petersburg (South Coast)",
    "Warsaw",
    "Sevastopol",
    "Norwegian Sea",
    "Yorkshire",
    "North Sea",
    "Spain",
    "Picardy",
    "Mid-Atlantic Ocean",
    "Tyrrhenian Sea",
    "Silesia",
    "Holland",
    "Albania",
    "Serbia",
    "Bulgaria",
    "St. Petersburg",
    "Gulf of Bothnia",
    "Ukraine",
    "Norway",
    "Belgium",
    "Portugal",
    "Piedmont",
    "Tunis",
    "Galicia",
    "Greece",
    "Black Sea",
    "Rumania",
    "Sweden",
    "Finland",
    "Helgoland Bight",
    "Burgundy",
    "English Channel",
    "Tyrolia",
    "Ionian Sea",
    "Denmark",
    "Adriatic Sea",
    "Wales",
    "Irish Sea",
    "Armenia",
    "St. Petersburg (North Coast)",
    "North Atlantic Ocean",
    "Ruhr",
    "Apulia",
    "Aegean Sea",
    "Skagerrack",
    "Gascony",
    "Prussia",
    "Barents Sea",
    "Bohemia",
    "Western Mediterranean",
    "Tuscany",
    "Baltic Sea",
    "Gulf of Lyons",
    "Eastern Mediterranean",
    "Spain (North Coast)",
    "Livonia",
    "Syria",
    "Clyde",
    "North Africa",
    "Spain (South Coast)",
    "Bulgaria (South Coast)",
    "Bulgaria (East Coast)",
    "Switzerland"
  )

  def contains(location: String) : Boolean = {
    return allowedLocations.contains(location)
  }
}
