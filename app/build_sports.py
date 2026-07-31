import re

sports_data = [
    # EASY (SPO001 - SPO040)
    ("SPO001", "sports", "In cricket, how many overs are bowled in a single innings of a standard T20 International match per side?", "10", "20", "50", "40", "20", "A standard Twenty20 (T20) international match limits each side to 20 overs.", "Easy"),
    ("SPO002", "sports", "Which country won the inaugural FIFA Men's World Cup in 1930?", "Brazil", "Argentina", "Uruguay", "Italy", "Uruguay", "Uruguay defeated Argentina 4-2 in Montevideo to win the inaugural 1930 World Cup.", "Easy"),
    ("SPO003", "sports", "In basketball, how many points is a field goal worth if made from beyond the three-point arc?", "1 point", "2 points", "3 points", "4 points", "3 points", "Shots made beyond the three-point line award three points in basketball.", "Easy"),
    ("SPO004", "sports", "Which grand slam tennis tournament is played on traditional grass courts in London?", "Australian Open", "French Open", "Wimbledon", "US Open", "Wimbledon", "Wimbledon is the oldest tennis tournament in the world, played on outdoor grass courts.", "Easy"),
    ("SPO005", "sports", "How many players are on the field for one team during a standard soccer match?", "9", "10", "11", "12", "11", "A standard soccer team fields 11 players including the goalkeeper.", "Easy"),
    ("SPO006", "sports", "In field hockey, what is the maximum number of players permitted on the pitch for a single team at one time?", "11", "9", "10", "12", "11", "A field hockey team has 11 active players on the pitch during play.", "Easy"),
    ("SPO007", "sports", "Which country developed the modern rules of badminton in the 19th century?", "China", "India", "Great Britain", "Indonesia", "Great Britain", "Modern badminton rules were developed in mid-19th century British India and standardized in England.", "Easy"),
    ("SPO008", "sports", "In indoor volleyball, how many players per team are on the court during active play?", "5", "6", "7", "8", "6", "Indoor volleyball is played by two teams of six players each.", "Easy"),
    ("SPO009", "sports", "In baseball, how many strikes result in a batter being called out?", "2", "3", "4", "5", "3", "Three strikes result in a strikeout in baseball.", "Easy"),
    ("SPO010", "sports", "Who holds the world record for the fastest 100-meter sprint in men's athletics (9.58 seconds)?", "Carl Lewis", "Usain Bolt", "Tyson Gay", "Yohan Blake", "Usain Bolt", "Usain Bolt set the 100m world record of 9.58 seconds at the 2009 Berlin World Championships.", "Easy"),
    ("SPO011", "sports", "How often are the modern Summer Olympic Games held?", "Every 2 years", "Every 3 years", "Every 4 years", "Every 5 years", "Every 4 years", "The Summer Olympic Games occur quadrennially every four years.", "Easy"),
    ("SPO012", "sports", "Which chess piece can move any number of squares diagonally?", "Rook", "Bishop", "Knight", "Pawn", "Bishop", "The bishop moves diagonally across unblocked squares of the same color.", "Easy"),
    ("SPO013", "sports", "In Formula 1 racing, what flag is waved to signal the end of the race?", "Yellow Flag", "Red Flag", "Chequered Flag", "Green Flag", "Chequered Flag", "The black and white chequered flag signals the end of a session or race.", "Easy"),
    ("SPO014", "sports", "In Kabaddi, how many players from each team are on the court at the start of a match?", "5", "6", "7", "8", "7", "Each Kabaddi team consists of seven active players on the court.", "Easy"),
    ("SPO015", "sports", "How many Grand Slam tournaments are held annually in professional tennis?", "3", "4", "5", "6", "4", "The four Grand Slams are the Australian Open, French Open, Wimbledon, and US Open.", "Easy"),
    ("SPO016", "sports", "In boxing, what does the abbreviation 'KO' stand for?", "Knockout", "Kickout", "Keyout", "Keepout", "Knockout", "KO occurs when a fighter cannot rise before the count of ten.", "Easy"),
    ("SPO017", "sports", "Which swimming stroke features an overhead arm movement paired with a dolphin kick?", "Breaststroke", "Backstroke", "Butterfly stroke", "Freestyle", "Butterfly stroke", "The butterfly stroke uses simultaneous arm recovery and dolphin leg kicks.", "Easy"),
    ("SPO018", "sports", "Which legendary Brazilian footballer won three FIFA World Cups (1958, 1962, 1970)?", "Diego Maradona", "Pelé", "Johan Cruyff", "Zinedine Zidane", "Pelé", "Pelé is the only player to have won three FIFA World Cup tournaments.", "Easy"),
    ("SPO019", "sports", "In cricket, how many runs are awarded if a hit ball clears the boundary rope on the fly?", "4 runs", "5 runs", "6 runs", "8 runs", "6 runs", "Clearing the boundary without bouncing yields six runs.", "Easy"),
    ("SPO020", "sports", "Which city hosted the 2024 Summer Olympic Games?", "Tokyo", "Paris", "Los Angeles", "London", "Paris", "Paris hosted the Games of the XXXIII Olympiad in July-August 2024.", "Easy"),
    ("SPO021", "sports", "In basketball, what is the maximum duration an offensive player may remain in the key area continuously?", "3 seconds", "5 seconds", "8 seconds", "10 seconds", "3 seconds", "The three-second rule restricts offensive players inside the key.", "Easy"),
    ("SPO022", "sports", "What surface is the French Open tennis tournament played on at Roland Garros?", "Grass", "Red Clay", "Hard Court", "Carpet", "Red Clay", "Roland Garros is played on crushed brick red clay courts.", "Easy"),
    ("SPO023", "sports", "In baseball, how many total bases make up the infield diamond path?", "3 bases", "4 bases", "5 bases", "6 bases", "4 bases", "The bases are first, second, third, and home plate.", "Easy"),
    ("SPO024", "sports", "Which nation won the ICC Men's Cricket World Cup in 2011?", "Sri Lanka", "Australia", "India", "England", "India", "India won their second ODI World Cup in 2011 under MS Dhoni's captaincy.", "Easy"),
    ("SPO025", "sports", "In ice hockey, what is the vulcanized rubber disc used in place of a ball called?", "Puck", "Disc", "Stone", "Ring", "Puck", "Ice hockey is played with a heavy vulcanized rubber puck.", "Easy"),
    ("SPO026", "sports", "What is the regulation length of an Olympic-sized swimming pool?", "25 meters", "50 meters", "75 meters", "100 meters", "50 meters", "Olympic standard pools measure 50 meters in length.", "Easy"),
    ("SPO027", "sports", "What highest title is awarded to elite chess players by FIDE?", "Grandmaster", "Master", "Champion", "Lord", "Grandmaster", "Grandmaster (GM) is the highest title in international chess.", "Easy"),
    ("SPO028", "sports", "Which country has won the most Gold medals in men's field hockey in Olympic history?", "Pakistan", "India", "Netherlands", "Australia", "India", "India has won eight Olympic gold medals in men's field hockey.", "Easy"),
    ("SPO029", "sports", "In golf, what is the score called when a player completes a hole in one stroke under par?", "Eagle", "Birdie", "Bogey", "Albatross", "Birdie", "One stroke below par on an individual hole is called a birdie.", "Easy"),
    ("SPO030", "sports", "Which Formula 1 team is famous for its 'Prancing Horse' emblem and red car color?", "Mercedes", "Red Bull Racing", "Ferrari", "McLaren", "Ferrari", "Scuderia Ferrari is F1's oldest and most successful team.", "Easy"),
    ("SPO031", "sports", "In table tennis, how many points are required to win a standard set (win by 2)?", "11 points", "15 points", "21 points", "25 points", "11 points", "Modern table tennis games are played to 11 points.", "Easy"),
    ("SPO032", "sports", "In American football, how many points is a touchdown worth?", "3 points", "6 points", "7 points", "8 points", "6 points", "A touchdown earns six points before conversion attempts.", "Easy"),
    ("SPO033", "sports", "Which Portuguese star holds the record for the most international goals in men's soccer history?", "Lionel Messi", "Cristiano Ronaldo", "Neymar Jr.", "Kylian Mbappé", "Cristiano Ronaldo", "Cristiano Ronaldo has scored over 130 international goals for Portugal.", "Easy"),
    ("SPO034", "sports", "Which sport uses a racket and a feathered or synthetic shuttlecock?", "Squash", "Badminton", "Tennis", "Pickleball", "Badminton", "Badminton is played with light rackets and a shuttlecock.", "Easy"),
    ("SPO035", "sports", "In athletics, how many runners compete on a standard relay team?", "2 runners", "3 runners", "4 runners", "5 runners", "4 runners", "Relay races like the 4x100m feature four team members.", "Easy"),
    ("SPO036", "sports", "Which nation won back-to-back FIFA Women's World Cup titles in 2015 and 2019?", "Germany", "United States", "Japan", "England", "United States", "The USWNT won consecutive Women's World Cups in 2015 and 2019.", "Easy"),
    ("SPO037", "sports", "In cricket, what term is used when a bowler dismisses three batsmen on consecutive deliveries?", "Maiden", "Century", "Hat-trick", "Five-for", "Hat-trick", "Dismissing three batsmen on consecutive balls is a hat-trick.", "Easy"),
    ("SPO038", "sports", "Which boxing legend was famous for his phrase 'Float like a butterfly, sting like a bee'?", "Mike Tyson", "Muhammad Ali", "Joe Frazier", "George Foreman", "Muhammad Ali", "Muhammad Ali is widely regarded as one of the greatest heavyweights.", "Easy"),
    ("SPO039", "sports", "What color jersey identifies the overall leader in the Tour de France?", "Green Jersey", "Pink Jersey", "Yellow Jersey", "White Jersey", "Yellow Jersey", "The maillot jaune (yellow jersey) is worn by the general classification leader.", "Easy"),
    ("SPO040", "sports", "In target archery, what color is the central bullseye area?", "Red", "Yellow", "Blue", "Black", "Yellow", "The innermost 10-point ring on a standard target face is yellow.", "Easy"),

    # MEDIUM (SPO041 - SPO080)
    ("SPO041", "sports", "Who was the first batsman to score a double century (200*) in Men's One Day International (ODI) cricket?", "Virender Sehwag", "Sachin Tendulkar", "Rohit Sharma", "Chris Gayle", "Sachin Tendulkar", "Sachin Tendulkar scored 200 not out against South Africa in Gwalior in February 2010.", "Medium"),
    ("SPO042", "sports", "Which nation won the 2022 FIFA Men's World Cup title in Qatar?", "France", "Argentina", "Croatia", "Brazil", "Argentina", "Argentina defeated France on penalties after a dramatic 3-3 final.", "Medium"),
    ("SPO043", "sports", "In NBA basketball, how long is the shot clock for an offensive possession?", "14 seconds", "24 seconds", "30 seconds", "35 seconds", "24 seconds", "The NBA shot clock requires a shot attempt within 24 seconds.", "Medium"),
    ("SPO044", "sports", "Who holds the record for the most men's Grand Slam singles titles in tennis history?", "Roger Federer", "Rafael Nadal", "Novak Djokovic", "Pete Sampras", "Novak Djokovic", "Novak Djokovic holds 24 men's Grand Slam singles titles.", "Medium"),
    ("SPO045", "sports", "Which athlete set world records in men's pole vault clearing over 6.20 meters?", "Sergey Bubka", "Armand Duplantis", "Renaud Lavillenie", "Thiago Braz", "Armand Duplantis", "Armand 'Mondo' Duplantis broke multiple pole vault world records.", "Medium"),
    ("SPO046", "sports", "Who holds the record for the most Formula 1 Grand Prix race wins (over 100)?", "Michael Schumacher", "Lewis Hamilton", "Ayrton Senna", "Max Verstappen", "Lewis Hamilton", "Sir Lewis Hamilton has won over 100 Formula 1 Grand Prix races.", "Medium"),
    ("SPO047", "sports", "Which Indian wrestler won a Silver medal in men's freestyle 57kg at the Tokyo 2020 Olympics?", "Bajrang Punia", "Ravi Kumar Dahiya", "Sushil Kumar", "Yogeshwar Dutt", "Ravi Kumar Dahiya", "Ravi Kumar Dahiya won silver in the 57kg freestyle wrestling category.", "Medium"),
    ("SPO048", "sports", "In chess, what special pawn capture move can occur immediately after a pawn advances two squares?", "Promotion", "En Passant", "Castling", "Fork", "En Passant", "En passant allows a pawn to capture an enemy pawn that bypassed it.", "Medium"),
    ("SPO049", "sports", "Which country won the ICC Men's T20 World Cup in 2024?", "South Africa", "India", "England", "Australia", "India", "India defeated South Africa in the 2024 T20 World Cup final in Barbados.", "Medium"),
    ("SPO050", "sports", "What is the official marathon race distance in kilometers?", "21.097 km", "42.195 km", "50.000 km", "30.000 km", "42.195 km", "The standard marathon distance is 42.195 kilometers (26 miles 385 yards).", "Medium"),
    ("SPO051", "sports", "Who became the first Indian track and field athlete to win Olympic Gold (Tokyo 2020)?", "Milkha Singh", "Neeraj Chopra", "Abhinav Bindra", "PT Usha", "Neeraj Chopra", "Neeraj Chopra won gold in Men's Javelin throw at Tokyo 2020.", "Medium"),
    ("SPO052", "sports", "Which football club won the UEFA Champions League title in 2024?", "Manchester City", "Real Madrid", "Borussia Dortmund", "Bayern Munich", "Real Madrid", "Real Madrid won their 15th European Cup/Champions League title in 2024.", "Medium"),
    ("SPO053", "sports", "In Kabaddi, what is the maximum duration allowed for a single raid?", "20 seconds", "30 seconds", "45 seconds", "60 seconds", "30 seconds", "Raiders have a maximum of 30 seconds to complete a raid.", "Medium"),
    ("SPO054", "sports", "Who became the youngest undisputed World Chess Champion at age 22 in 1985?", "Magnus Carlsen", "Garry Kasparov", "Anatoly Karpov", "Bobby Fischer", "Garry Kasparov", "Garry Kasparov defeated Anatoly Karpov in 1985 to become champion.", "Medium"),
    ("SPO055", "sports", "Which Indian player hit a world-record badminton smash clocked at 565 km/h?", "Satwiksairaj Rankireddy", "Chirag Shetty", "Lakshya Sen", "Kidambi Srikanth", "Satwiksairaj Rankireddy", "Satwiksairaj set the Guinness World Record for fastest badminton smash.", "Medium"),
    ("SPO056", "sports", "Which team won the inaugural season of the Indian Premier League (IPL) in 2008?", "Chennai Super Kings", "Mumbai Indians", "Rajasthan Royals", "Kolkata Knight Riders", "Rajasthan Royals", "Rajasthan Royals, led by Shane Warne, won the inaugural IPL in 2008.", "Medium"),
    ("SPO057", "sports", "In golf, what is the term for scoring two strokes under par on a single hole?", "Birdie", "Eagle", "Albatross", "Bogey", "Eagle", "An eagle is achieved by completing a hole two strokes under par.", "Medium"),
    ("SPO058", "sports", "Which nation won five consecutive Olympic Gold medals in Men's Basketball from 2008 to 2024?", "Spain", "France", "Serbia", "United States", "United States", "The USA Men's basketball team won gold in 2008, 2012, 2016, 2020, and 2024.", "Medium"),
    ("SPO059", "sports", "Who holds the record for the highest individual score in a Test cricket innings (400 not out)?", "Brian Lara", "Matthew Hayden", "Donald Bradman", "Virender Sehwag", "Brian Lara", "Brian Lara scored 400* for West Indies against England in 2004.", "Medium"),
    ("SPO060", "sports", "Which swimmer holds the record for the most Olympic medals of all time (28 medals)?", "Ryan Lochte", "Michael Phelps", "Caeleb Dressel", "Mark Spitz", "Michael Phelps", "Michael Phelps won 28 Olympic medals including 23 gold medals.", "Medium"),
    ("SPO061", "sports", "In professional boxing, what weight class has a upper limit of 147 pounds (66.7 kg)?", "Featherweight", "Lightweight", "Welterweight", "Middleweight", "Welterweight", "The welterweight division limit is set at 147 lbs.", "Medium"),
    ("SPO062", "sports", "Which venue in London is widely referred to as the 'Home of Cricket'?", "Melbourne Cricket Ground", "Lord's Cricket Ground", "Eden Gardens", "The Oval", "Lord's Cricket Ground", "Lord's, established in 1814, is known as the Home of Cricket.", "Medium"),
    ("SPO063", "sports", "In indoor volleyball, how many points are needed to win sets 1 through 4?", "21 points", "25 points", "30 points", "15 points", "25 points", "Sets 1-4 require 25 points with a minimum two-point margin.", "Medium"),
    ("SPO064", "sports", "Which nation hosted and won the 1995 Rugby World Cup?", "Australia", "New Zealand", "South Africa", "England", "South Africa", "South Africa's Springboks won the 1995 Rugby World Cup on home soil.", "Medium"),
    ("SPO065", "sports", "In baseball, what is a home run hit when all three bases are loaded called?", "Grand Slam", "Triple Play", "Clean Sweep", "Home Run Derby", "Grand Slam", "A home run with loaded bases scores four runs and is a Grand Slam.", "Medium"),
    ("SPO066", "sports", "Who has won the male Ballon d'Or award a record eight times?", "Cristiano Ronaldo", "Lionel Messi", "Michel Platini", "Johan Cruyff", "Lionel Messi", "Lionel Messi won eight Ballon d'Or trophies between 2009 and 2023.", "Medium"),
    ("SPO067", "sports", "Which tennis icon achieved a calendar-year 'Golden Slam' by winning all four Majors and Olympic Gold in 1988?", "Serena Williams", "Steffi Graf", "Martina Navratilova", "Chris Evert", "Steffi Graf", "Steffi Graf achieved the unique Golden Slam in 1988.", "Medium"),
    ("SPO068", "sports", "Who won four consecutive F1 Drivers' Championships with Red Bull from 2010 to 2013?", "Sebastian Vettel", "Lewis Hamilton", "Fernando Alonso", "Nico Rosberg", "Sebastian Vettel", "Sebastian Vettel dominated F1 with four titles from 2010 to 2013.", "Medium"),
    ("SPO069", "sports", "Which team won the inaugural season of the Pro Kabaddi League (PKL) in 2014?", "U Mumba", "Jaipur Pink Panthers", "Bengaluru Bulls", "Patna Pirates", "Jaipur Pink Panthers", "Jaipur Pink Panthers defeated U Mumba to win the 2014 PKL.", "Medium"),
    ("SPO070", "sports", "Who was the first bowler in cricket history to take 800 Test wickets?", "Shane Warne", "Muttiah Muralitharan", "Anil Kumble", "James Anderson", "Muttiah Muralitharan", "Sri Lanka's Muralitharan retired with 800 Test wickets.", "Medium"),
    ("SPO071", "sports", "Which country won the 1966 FIFA Men's World Cup as the host nation?", "West Germany", "Brazil", "England", "Argentina", "England", "England defeated West Germany 4-2 in the 1966 final at Wembley.", "Medium"),
    ("SPO072", "sports", "In fencing, which weapon restricts valid target area strictly to the torso?", "Épée", "Foil", "Sabre", "Dagger", "Foil", "The foil target area is limited to the opponent's torso.", "Medium"),
    ("SPO073", "sports", "Who holds the career record for the highest Test cricket batting average (99.94)?", "Sachin Tendulkar", "Sir Donald Bradman", "Ricky Ponting", "Jacques Kallis", "Sir Donald Bradman", "Sir Don Bradman averaged 99.94 across 52 Test matches.", "Medium"),
    ("SPO074", "sports", "Which Indian gymnast placed 4th at the 2016 Rio Olympics performing the Produnova vault?", "Dipa Karmakar", "Pranati Nayak", "Aruna Reddy", "Bula Choudhury", "Dipa Karmakar", "Dipa Karmakar won praise for executing the difficult Produnova vault.", "Medium"),
    ("SPO075", "sports", "In water polo, how many active players per team are in the pool at one time?", "5", "6", "7", "8", "7", "Water polo teams field six field players and one goalkeeper.", "Medium"),
    ("SPO076", "sports", "Which nation hosted the first FIFA Men's World Cup held on the African continent in 2010?", "Egypt", "Nigeria", "South Africa", "Morocco", "South Africa", "South Africa hosted the 2010 FIFA World Cup.", "Medium"),
    ("SPO077", "sports", "Who became the youngest FIDE Candidates Tournament winner in chess history in 2024 at age 17?", "Dommaraju Gukesh", "Rameshbabu Praggnanandhaa", "Arjun Erigaisi", "Alireza Firouzja", "Dommaraju Gukesh", "Gukesh D. won the 2024 Candidates Tournament in Toronto.", "Medium"),
    ("SPO078", "sports", "In Formula 1, what does the acronym 'DRS' stand for?", "Drag Reduction System", "Direct Race Strategy", "Dynamic Recovery System", "Drive Response Speed", "Drag Reduction System", "DRS adjusts the rear wing to increase overtaking speed.", "Medium"),
    ("SPO079", "sports", "Which nation won the Gold medal in Women's Field Hockey at the Tokyo 2020 Olympics?", "Argentina", "Netherlands", "Great Britain", "Australia", "Netherlands", "The Netherlands women's team defeated Argentina for gold in Tokyo.", "Medium"),
    ("SPO080", "sports", "What is the distance between the pitcher's rubber and home plate in Major League Baseball?", "50 feet", "60 feet 6 inches", "65 feet", "70 feet", "60 feet 6 inches", "The pitching distance is exactly 60 feet 6 inches (18.44 m).", "Medium"),

    # HARD (SPO081 - SPO100)
    ("SPO081", "sports", "Who was the first goalkeeper to win the FIFA World Cup Golden Ball award for best player (2002)?", "Gianluigi Buffon", "Oliver Kahn", "Iker Casillas", "Manuel Neuer", "Oliver Kahn", "Germany's Oliver Kahn won the Golden Ball at the 2002 FIFA World Cup.", "Hard"),
    ("SPO082", "sports", "Who recorded the fastest century in Test cricket history off just 54 balls in 2016?", "Viv Richards", "Brendon McCullum", "Adam Gilchrist", "Shahid Afridi", "Brendon McCullum", "New Zealand's Brendon McCullum scored a 54-ball hundred against Australia.", "Hard"),
    ("SPO083", "sports", "In standard chess setup, what square coordinate is occupied by the White King at game start?", "d1", "e1", "d8", "e8", "e1", "White's King starts on the e1 square on a standard chessboard.", "Hard"),
    ("SPO084", "sports", "Which long jumper broke the world record with an 8.95-meter jump at the 1991 Tokyo World Championships?", "Bob Beamon", "Mike Powell", "Carl Lewis", "Jonathan Edwards", "Mike Powell", "Mike Powell broke Bob Beamon's 23-year-old world record in 1991.", "Hard"),
    ("SPO085", "sports", "Who is the only Formula 1 driver to win World Championships with three different constructors?", "Jack Brabham", "Graham Hill", "Juan Manuel Fangio", "Jim Clark", "Juan Manuel Fangio", "Fangio won F1 titles with Alfa Romeo, Maserati, Mercedes, and Ferrari.", "Hard"),
    ("SPO086", "sports", "What is the standard target distance in outdoor Olympic archery competitions?", "50 meters", "60 meters", "70 meters", "90 meters", "70 meters", "Olympic outdoor archery targets are positioned 70 meters away.", "Hard"),
    ("SPO087", "sports", "Which nation won the inaugural ICC Men's World Test Championship in June 2021?", "Australia", "New Zealand", "England", "South Africa", "New Zealand", "New Zealand defeated India in Southampton to win the WTC final.", "Hard"),
    ("SPO088", "sports", "How many athletes crew an Olympic coxless four rowing boat?", "2 rowers", "4 rowers", "6 rowers", "8 rowers", "4 rowers", "A coxless four features four rowers each with one oar and no coxswain.", "Hard"),
    ("SPO089", "sports", "Who was the first female tennis player to achieve a Career Grand Slam in singles during the Open Era?", "Serena Williams", "Steffi Graf", "Margaret Court", "Chris Evert", "Margaret Court", "Margaret Court completed her career Grand Slam in 1963.", "Hard"),
    ("SPO090", "sports", "In Kabaddi, how many defenders must be on the mat for a raider to earn a Bonus Point?", "At least 4", "At least 5", "At least 6", "At least 7", "At least 6", "The bonus line is active only when six or seven defenders are present.", "Hard"),
    ("SPO091", "sports", "Who won India's first individual Olympic Gold medal (10m Air Rifle) at Beijing 2008?", "Rajyavardhan Singh Rathore", "Abhinav Bindra", "Gagan Narang", "Vijay Kumar", "Abhinav Bindra", "Abhinav Bindra won India's first individual Olympic gold in 2008.", "Hard"),
    ("SPO092", "sports", "How many events comprise the men's Olympic decathlon in track and field?", "8 events", "10 events", "12 events", "15 events", "10 events", "Decathlon consists of ten combined track and field events.", "Hard"),
    ("SPO093", "sports", "In which city was the famous 1986 Argentina vs England World Cup quarter-final played?", "Guadalajara", "Mexico City", "Monterrey", "Puebla", "Mexico City", "The match featuring Maradona's iconic goals took place at Estadio Azteca in Mexico City.", "Hard"),
    ("SPO094", "sports", "In badminton, what is the exact regulation height of the net at the center of the court?", "1.524 meters", "1.550 meters", "1.600 meters", "1.480 meters", "1.524 meters", "The net height is 1.524 meters (5 feet) at the center.", "Hard"),
    ("SPO095", "sports", "Which bowler was the first in Test history to take all 10 wickets in a single innings (1956)?", "Anil Kumble", "Jim Laker", "Ajaz Patel", "Shane Warne", "Jim Laker", "Jim Laker took 10 for 53 for England against Australia at Old Trafford.", "Hard"),
    ("SPO096", "sports", "Who is the youngest driver to win a Formula 1 Grand Prix race (18 years, 228 days)?", "Sebastian Vettel", "Max Verstappen", "Fernando Alonso", "Charles Leclerc", "Max Verstappen", "Max Verstappen won the 2016 Spanish GP on his Red Bull debut.", "Hard"),
    ("SPO097", "sports", "In Olympic freestyle wrestling, how many points are awarded for a high-amplitude throw?", "2 points", "4 points", "5 points", "3 points", "5 points", "A grand amplitude throw directly bringing an opponent to risk awards 5 points.", "Hard"),
    ("SPO098", "sports", "Which country won the Gold medal in Men's Volleyball at the Tokyo 2020 Olympics?", "Brazil", "France", "ROC", "Poland", "France", "France defeated ROC 3-2 in the Tokyo 2020 Men's Volleyball final.", "Hard"),
    ("SPO099", "sports", "Which sprinter won gold in both 200m and 400m at the 1996 Atlanta Olympics?", "Marie-José Pérec", "Florence Griffith-Joyner", "Allyson Felix", "Cathy Freeman", "Marie-José Pérec", "France's Marie-José Pérec achieved the rare 200m/400m Olympic double.", "Hard"),
    ("SPO100", "sports", "In chess endgames, what German term describes a situation where a player is forced to move to their disadvantage?", "Zugzwang", "Stalemate", "Zwischenzug", "Fianchetto", "Zugzwang", "Zugzwang occurs when any legal move available weakens a player's position.", "Hard")
]

print(f'Total questions generated: {len(sports_data)}')

ids = [q[0] for q in sports_data]
assert len(set(ids)) == 100, f'Duplicate IDs! Unique: {len(set(ids))}'

questions = [q[2] for q in sports_data]
assert len(set(questions)) == 100, f'Duplicate questions! Unique: {len(set(questions))}'

easies = [q for q in sports_data if q[9] == 'Easy']
mediums = [q for q in sports_data if q[9] == 'Medium']
hards = [q for q in sports_data if q[9] == 'Hard']

print(f'Easy count: {len(easies)} (SPO001-SPO040)')
print(f'Medium count: {len(mediums)} (SPO041-SPO080)')
print(f'Hard count: {len(hards)} (SPO081-SPO100)')

assert len(easies) == 40, f'Expected 40 Easy, got {len(easies)}'
assert len(mediums) == 40, f'Expected 40 Medium, got {len(mediums)}'
assert len(hards) == 20, f'Expected 20 Hard, got {len(hards)}'

for i, q in enumerate(sports_data):
    opts = [q[3], q[4], q[5], q[6]]
    assert len(set(opts)) == 4, f'Duplicate options in {q[0]}: {opts}'
    assert q[7] in opts, f'Correct answer {q[7]} not in options for {q[0]}'

print('All python checks passed successfully!')

# Generate Kotlin string
kotlin_lines = ["    private val sportsSeeds = listOf("]
for i, q in enumerate(sports_data):
    comma = "," if i < len(sports_data) - 1 else ""
    # escape quotes
    q_id, cat, text, opA, opB, opC, opD, ans, exp, diff = q
    text_e = text.replace('"', '\\"')
    opA_e = opA.replace('"', '\\"')
    opB_e = opB.replace('"', '\\"')
    opC_e = opC.replace('"', '\\"')
    opD_e = opD.replace('"', '\\"')
    ans_e = ans.replace('"', '\\"')
    exp_e = exp.replace('"', '\\"')
    line = f'        QuestionEntity("{q_id}", "{cat}", "{text_e}", "{opA_e}", "{opB_e}", "{opC_e}", "{opD_e}", "{ans_e}", "{exp_e}", "{diff}"){comma}'
    kotlin_lines.append(line)
kotlin_lines.append("    )")

kotlin_code = "\n".join(kotlin_lines)

with open('sports_kotlin.txt', 'w') as f:
    f.write(kotlin_code)

print("Saved sports_kotlin.txt successfully!")
