import sys
import re

# MOV101 to MOV200 (100 questions total)
# 40 Easy (MOV101-MOV140)
# 40 Medium (MOV141-MOV180)
# 20 Hard (MOV181-MOV200)

movies_batch2 = [
    # EASY (MOV101 - MOV140)
    ("MOV101", "movies", "What is the highest award presented at the prestigious Cannes Film Festival?", "Palme d'Or", "Golden Lion", "Golden Bear", "Crystal Globe", "Palme d'Or", "The Palme d'Or is the highest prize awarded at the Cannes Film Festival since 1955.", "Easy"),
    ("MOV102", "movies", "Which famous British film awards ceremony is considered the UK equivalent of the Oscars?", "BAFTA Awards", "César Awards", "Goya Awards", "AACTA Awards", "BAFTA Awards", "The British Academy Film Awards are presented annually by BAFTA.", "Easy"),
    ("MOV103", "movies", "Which German film festival presents top winners with the 'Golden Bear' trophy?", "Berlin International Film Festival", "Venice Film Festival", "Locarno Film Festival", "Rotterdam Film Festival", "Berlin International Film Festival", "The Golden Bear is the highest award for best film at the Berlinale.", "Easy"),
    ("MOV104", "movies", "Which Italian film festival, established in 1932, is the oldest film festival in the world?", "Venice Film Festival", "Cannes Film Festival", "Rome Film Festival", "Turin Film Festival", "Venice Film Festival", "The Venice International Film Festival is held annually on the island of the Lido in Venice.", "Easy"),
    ("MOV105", "movies", "What animal serves as the signature trophy awarded at the Venice Film Festival?", "Golden Lion", "Golden Bear", "Golden Leopard", "Golden Eagle", "Golden Lion", "The Golden Lion (Leone d'Oro) is the prestigious main prize of Venice.", "Easy"),
    ("MOV106", "movies", "Which Hollywood film studio created beloved animated movies like 'Shrek', 'Madagascar', and 'How to Train Your Dragon'?", "DreamWorks Animation", "Pixar", "Illumination", "Blue Sky Studios", "DreamWorks Animation", "DreamWorks Animation was founded by Steven Spielberg, Jeffrey Katzenberg, and David Geffen.", "Easy"),
    ("MOV107", "movies", "Which German composer produced iconic orchestral scores for 'The Dark Knight', 'Gladiator', and 'Inception'?", "Hans Zimmer", "John Williams", "Ennio Morricone", "Howard Shore", "Hans Zimmer", "Hans Zimmer is renowned for blending electronic synthesis with traditional orchestral arrangements.", "Easy"),
    ("MOV108", "movies", "Which 2004 Christmas animated film starring Tom Hanks was recorded entirely using full motion-capture technology?", "The Polar Express", "Beowulf", "A Christmas Carol", "Monster House", "The Polar Express", "The Polar Express is listed in the Guinness World Records as the first all-digital capture film.", "Easy"),
    ("MOV109", "movies", "Which legendary film director directed iconic sci-fi thrillers like 'Alien' (1979) and 'Blade Runner' (1982)?", "Ridley Scott", "James Cameron", "George Lucas", "John Carpenter", "Ridley Scott", "Ridley Scott is a master of visual atmosphere and influential world-building in cinema.", "Easy"),
    ("MOV110", "movies", "Which animation studio produced hit films such as 'Despicable Me', 'Minions', and 'The Super Mario Bros. Movie'?", "Illumination", "Pixar", "DreamWorks", "Sony Pictures Animation", "Illumination", "Illumination was founded by Chris Meledandri in 2007.", "Easy"),
    ("MOV111", "movies", "What visual frame rate is standard for traditional cinematic projection in movie theaters worldwide?", "24 frames per second", "30 frames per second", "60 frames per second", "120 frames per second", "24 frames per second", "24 fps has been the standardized frame rate for sound film projection since the late 1920s.", "Easy"),
    ("MOV112", "movies", "Which famous Hollywood screenwriter wrote acclaimed films like 'The Social Network' and 'A Few Good Men'?", "Aaron Sorkin", "Quentin Tarantino", "Charlie Kaufman", "William Goldman", "Aaron Sorkin", "Aaron Sorkin is celebrated for fast-paced, rhythmic dialogue and walk-and-talk sequences.", "Easy"),
    ("MOV113", "movies", "Which film award ceremony is hosted annually by the Hollywood Foreign Press Association (or Dick Clark Productions)?", "Golden Globe Awards", "Screen Actors Guild Awards", "Critics' Choice Awards", "People's Choice Awards", "Golden Globe Awards", "The Golden Globes honor achievements in both film and television.", "Easy"),
    ("MOV114", "movies", "Which documentary film directed by Luc Jacquet won the Academy Award for Best Documentary Feature in 2006?", "March of the Penguins", "An Inconvenient Truth", "Free Solo", "My Octopus Teacher", "March of the Penguins", "March of the Penguins followed the annual journey of emperor penguins in Antarctica.", "Easy"),
    ("MOV115", "movies", "Which film format developed in Canada uses giant screen sizes and ultra-high-resolution projection?", "IMAX", "4DX", "RealD 3D", "Cinerama", "IMAX", "IMAX screens are famous for immersive aspect ratios and massive scale.", "Easy"),
    ("MOV116", "movies", "Who won the Best Director Oscar for 'Life of Pi' (2012) and 'Brokeback Mountain' (2005)?", "Ang Lee", "Bong Joon-ho", "Guillermo del Toro", "Chloé Zhao", "Ang Lee", "Taiwanese filmmaker Ang Lee won two Academy Awards for Best Director.", "Easy"),
    ("MOV117", "movies", "Which famous animation studio created iconic claymation characters Wallace and Gromit?", "Aardman Animations", "Laika", "Stop Motion Studio", "Pixar", "Aardman Animations", "Aardman Animations is a British studio famous for stop-motion clay animation.", "Easy"),
    ("MOV118", "movies", "Which 1997 James Cameron epic won 11 Academy Awards including Best Film Editing and Best Visual Effects?", "Titanic", "Avatar", "Terminator 2", "The Abyss", "Titanic", "Titanic shared the record of 11 Oscar wins with Ben-Hur and Lord of the Rings.", "Easy"),
    ("MOV119", "movies", "What is the green or blue screen technique called in film VFX used to replace backgrounds?", "Chroma Keying", "Rotoscoping", "Matte Painting", "Motion Capture", "Chroma Keying", "Chroma keying isolates color hue to make the background transparent.", "Easy"),
    ("MOV120", "movies", "Which Indian composer became famous globally for scoring the soundtrack to 'Slumdog Millionaire'?", "A. R. Rahman", "Ilaiyaraaja", "M. M. Keeravani", "Pritam", "A. R. Rahman", "A. R. Rahman won two Academy Awards for Slumdog Millionaire in 2009.", "Easy"),
    ("MOV121", "movies", "Which famous film crew member is directly responsible for operating the camera and camera movements during shooting?", "Camera Operator", "Key Grip", "Gaffer", "Best Boy", "Camera Operator", "The camera operator physically controls the camera under direction of the DP.", "Easy"),
    ("MOV122", "movies", "Which independent film festival held annually in Utah is the largest indie film festival in the United States?", "Sundance Film Festival", "Tribeca Film Festival", "SXSW Film Festival", "Telluride Film Festival", "Sundance Film Festival", "Sundance was co-founded by Robert Redford to highlight independent cinema.", "Easy"),
    ("MOV123", "movies", "Which stop-motion animation studio produced acclaimed dark fantasy feature films like 'Coraline' and 'Kubo and the Two Strings'?", "Laika", "Aardman", "Studio Ghibli", "Pixar", "Laika", "Laika Studios is known for blending traditional stop-motion with 3D printing technology.", "Easy"),
    ("MOV124", "movies", "Which 2018 documentary following rock climber Alex Honnold climbing El Capitan won the Academy Award?", "Free Solo", "The Dawn Wall", "Touching the Void", "14 Peaks", "Free Solo", "Free Solo won Best Documentary Feature at the 91st Academy Awards.", "Easy"),
    ("MOV125", "movies", "What film technical role is in charge of managing lighting equipment and electricians on a movie set?", "Gaffer", "Grip", "Boom Operator", "Foley Artist", "Gaffer", "The Gaffer works under the Director of Photography to design and set up lighting.", "Easy"),
    ("MOV126", "movies", "Which film award honours the worst achievements in film every year, presented the night before the Oscars?", "Golden Raspberry Awards (Razzie)", "Saturn Awards", "MTV Movie Awards", "Empire Awards", "Golden Raspberry Awards (Razzie)", "The Razzies parody Hollywood by awarding gold spray-painted raspberries.", "Easy"),
    ("MOV127", "movies", "What sound effect artist records live everyday sounds like footsteps and rustling clothes during post-production?", "Foley Artist", "Sound Designer", "Boom Operator", "Audio Engineer", "Foley Artist", "Foley artists replicate ambient environmental sound effects synced to picture.", "Easy"),
    ("MOV128", "movies", "Which French national film award ceremony is considered France's equivalent of the Academy Awards?", "César Award", "Goya Award", "Bavarian Film Award", "David di Donatello", "César Award", "The César Awards are presented annually by the Académie des Arts et Techniques du Cinéma.", "Easy"),
    ("MOV129", "movies", "Which visual effect technique involves painting realistic backgrounds onto glass or digital canvas for film environments?", "Matte Painting", "Chroma Key", "Morphing", "Optical Printing", "Matte Painting", "Matte paintings allow filmmakers to create grand fictional environments.", "Easy"),
    ("MOV130", "movies", "Which film director adapted J.R.R. Tolkien's epic fantasy trilogy 'The Lord of the Rings' into blockbuster movies?", "Peter Jackson", "Guillermo del Toro", "Sam Raimi", "David Yates", "Peter Jackson", "Peter Jackson shot all three Lord of the Rings films back-to-back in New Zealand.", "Easy"),
    ("MOV131", "movies", "What is the title of the sound recordist who holds the long microphone pole over actors during scene shoots?", "Boom Operator", "Foley Artist", "Sound Mixer", "Gaffer", "Boom Operator", "The boom operator positions the microphone near dialogue while keeping it out of frame.", "Easy"),
    ("MOV132", "movies", "Which Spanish film award presented by the Academy of Cinematographic Arts and Sciences is Spain's main national film award?", "Goya Award", "César Award", "Ariel Award", "Silver Condor", "Goya Award", "The Goya Awards are named after painter Francisco de Goya.", "Easy"),
    ("MOV133", "movies", "Which iconic Italian composer composed unforgettable soundtracks for spaghetti westerns like 'The Good, the Bad and the Ugly'?", "Ennio Morricone", "Nino Rota", "Giorgio Moroder", "Ludovico Einaudi", "Ennio Morricone", "Ennio Morricone composed scores for over 400 film and television projects.", "Easy"),
    ("MOV134", "movies", "Which studio developed pioneering computer-generated animation software and created 'Toy Story' in 1995?", "Pixar Animation Studios", "DreamWorks", "Sony Pictures Imageworks", "Industrial Light & Magic", "Pixar Animation Studios", "Pixar produced Toy Story, the world's first fully computer-animated feature film.", "Easy"),
    ("MOV135", "movies", "Which 2020 Netflix documentary series following exotic big cat breeders became a viral pop culture phenomenon?", "Tiger King", "Making a Murderer", "Wild Wild Country", "The Jinx", "Tiger King", "Tiger King garnered millions of viewers during global pandemic lockdowns.", "Easy"),
    ("MOV136", "movies", "What is the technique called where live footage is manually traced over frame-by-frame for animation?", "Rotoscoping", "Stop Motion", "Claymation", "CGI", "Rotoscoping", "Rotoscoping was invented by Max Fleischer in 1915 to create lifelike animated motion.", "Easy"),
    ("MOV137", "movies", "Which visual effects company founded by George Lucas in 1975 created groundbreaking VFX for 'Star Wars'?", "Industrial Light & Magic (ILM)", "Weta Digital", "Digital Domain", "Framestore", "Industrial Light & Magic (ILM)", "ILM introduced computer graphics, digital composite, and motion control cameras to cinema.", "Easy"),
    ("MOV138", "movies", "Which 2020 South African nature documentary won the Academy Award for Best Documentary Feature?", "My Octopus Teacher", "March of the Penguins", "Virunga", "The Cove", "My Octopus Teacher", "My Octopus Teacher documented Craig Foster's relationship with a wild octopus.", "Easy"),
    ("MOV139", "movies", "What is the piece of equipment called that holds physical film or cameras level on a wheeled track for smooth shots?", "Dolly", "Jib", "Gimbal", "Steadicam", "Dolly", "A camera dolly travels along tracks to capture smooth horizontal movement.", "Easy"),
    ("MOV140", "movies", "Which 2013 Mexican-American sci-fi thriller directed by Alfonso Cuarón won 7 Oscars including Best Film Editing?", "Gravity", "Interstellar", "Arrival", "The Martian", "Gravity", "Gravity starring Sandra Bullock won critical praise for revolutionary visual effects.", "Easy"),

    # MEDIUM (MOV141 - MOV180)
    ("MOV141", "movies", "Which New Zealand visual effects company co-founded by Peter Jackson produced landmark VFX for 'Avatar' and 'The Lord of the Rings'?", "Wētā FX (Weta Digital)", "Industrial Light & Magic", "Rythm & Hues", "Scanline VFX", "Wētā FX (Weta Digital)", "Wētā FX pioneered digital motion-capture and realistic digital character creation.", "Medium"),
    ("MOV142", "movies", "Who was the legendary cinematographer who shot visual masterpieces like 'Blade Runner 2049', '1917', and 'Skyfall'?", "Roger Deakins", "Emmanuel Lubezki", "Robert Richardson", "Hoyte van Hoytema", "Roger Deakins", "Roger Deakins won two Academy Awards after receiving 15 nominations.", "Medium"),
    ("MOV143", "movies", "Which Mexican cinematographer made Oscar history by winning Best Cinematography three consecutive years for 'Gravity', 'Birdman', and 'The Revenant'?", "Emmanuel Lubezki", "Guillermo Navarro", "Rodrigo Prieto", "Claudio Miranda", "Emmanuel Lubezki", "Emmanuel Lubezki is nicknamed 'Chivo' and celebrated for long continuous take shots.", "Medium"),
    ("MOV144", "movies", "Which 1977 sci-fi space opera won 6 Academy Awards and revolutionized sound design through sound editor Ben Burtt?", "Star Wars: Episode IV - A New Hope", "Close Encounters of the Third Kind", "Alien", "Star Trek: The Motion Picture", "Star Wars: Episode IV - A New Hope", "Ben Burtt created iconic sounds like the lightsaber hum and Chewbacca's roar.", "Medium"),
    ("MOV145", "movies", "Who composed the haunting Godfather theme and the memorable score for Federico Fellini's films?", "Nino Rota", "Ennio Morricone", "Henry Mancini", "Bernard Herrmann", "Nino Rota", "Nino Rota won the Academy Award for Best Original Score for The Godfather Part II.", "Medium"),
    ("MOV146", "movies", "What landmark mechanical camera stabilization rig invented by Garrett Brown in 1975 allows camera operators to walk smoothly without track?", "Steadicam", "Technocrane", "Russian Arm", "Ronin Gimbal", "Steadicam", "The Steadicam debuted in films like Bound for Glory, Rocky, and The Shining.", "Medium"),
    ("MOV147", "movies", "Which Japanese animated film directed by Hayao Miyazaki won the Golden Bear at Berlin in 2002 alongside the Best Animated Feature Oscar?", "Spirited Away", "Princess Mononoke", "Howl's Moving Castle", "My Neighbor Totoro", "Spirited Away", "Spirited Away is the only hand-drawn, non-English animated film to win the Oscar.", "Medium"),
    ("MOV148", "movies", "Which American screenwriter wrote the screenplays for classic thrillers 'Taxi Driver' (1976) and 'Raging Bull' (1980)?", "Paul Schrader", "Robert Towne", "Paddy Chayefsky", "William Goldman", "Paul Schrader", "Paul Schrader frequently collaborated with director Martin Scorsese.", "Medium"),
    ("MOV149", "movies", "Which iconic Hollywood film editor edited all of Martin Scorsese's major films including 'Raging Bull', 'Goodfellas', and 'The Departed'?", "Thelma Schoonmaker", "Sally Menke", "Anne V. Coates", "Margaret Sixel", "Thelma Schoonmaker", "Thelma Schoonmaker has won three Academy Awards for Best Film Editing.", "Medium"),
    ("MOV150", "movies", "Which 2015 action film directed by George Miller won 6 Academy Awards including Best Film Editing for Margaret Sixel?", "Mad Max: Fury Road", "The Revenant", "Sicario", "Furious 7", "Mad Max: Fury Road", "Mad Max: Fury Road was edited from over 480 hours of raw action footage.", "Medium"),
    ("MOV151", "movies", "Who composed the iconic psychological suspense score for Alfred Hitchcock's 'Psycho' (1960) using only string instruments?", "Bernard Herrmann", "Max Steiner", "Elmer Bernstein", "Dimitri Tiomkin", "Bernard Herrmann", "Bernard Herrmann used screeching violins to accentuate the famous shower scene.", "Medium"),
    ("MOV152", "movies", "Which film screenwriter won an Oscar for 'Chinatown' (1974), often cited by film schools as the gold standard of screenplay structure?", "Robert Towne", "Ernest Lehman", "Paddy Chayefsky", "Herman J. Mankiewicz", "Robert Towne", "Robert Towne's Chinatown screenplay is universally studied for mystery structure.", "Medium"),
    ("MOV153", "movies", "Which 2013 documentary directed by Joshua Oppenheimer explored former Indonesian death-squad leaders re-enacting their mass killings?", "The Act of Killing", "Citizenfour", "Cartel Land", "Restrepo", "The Act of Killing", "The Act of Killing won the BAFTA Award for Best Documentary and BAFTA nomination.", "Medium"),
    ("MOV154", "movies", "Which Dutch cinematographer shot sci-fi visuals for 'Interstellar', 'Dunkirk', 'Tenet', and 'Oppenheimer'?", "Hoyte van Hoytema", "Linus Sandgren", "Greig Fraser", "Roger Deakins", "Hoyte van Hoytema", "Hoyte van Hoytema won the Best Cinematography Oscar for Oppenheimer in 2024.", "Medium"),
    ("MOV155", "movies", "What is the technique where virtual characters are performed by real actors wearing sensor marker suits called?", "Performance Capture (Motion Capture)", "Stop Motion", "Rotoscoping", "Keyframe Animation", "Performance Capture (Motion Capture)", "Andy Serkis popularized performance capture playing Gollum and Caesar.", "Medium"),
    ("MOV156", "movies", "Which film editor won an Academy Award for editing Quentin Tarantino's 'Pulp Fiction' (1994)?", "Sally Menke", "Thelma Schoonmaker", "Carol Littleton", "Verna Fields", "Sally Menke", "Sally Menke edited all of Quentin Tarantino's films until her passing in 2010.", "Medium"),
    ("MOV157", "movies", "Which French new wave director directed the landmark 1960 crime film 'Breathless' (À bout de souffle)?", "Jean-Luc Godard", "François Truffaut", "Claude Chabrol", "Eric Rohmer", "Jean-Luc Godard", "Jean-Luc Godard popularized jump cuts and handheld camera techniques.", "Medium"),
    ("MOV158", "movies", "Which composer wrote the iconic theme music for James Bond films starting with 'Dr. No' and 'Goldfinger'?", "John Barry", "Monty Norman", "Bill Conti", "David Arnold", "John Barry", "John Barry arranged the original James Bond Theme and scored 11 Bond movies.", "Medium"),
    ("MOV159", "movies", "Which Australian cinematographer won the Academy Award for Best Cinematography for 'Dune: Part One' (2021)?", "Greig Fraser", "Roger Deakins", "Robert Richardson", "Dan Laustsen", "Greig Fraser", "Greig Fraser also served as Director of Photography on Rogue One and The Batman.", "Medium"),
    ("MOV160", "movies", "Which 2012 music documentary about singer Sixto Rodriguez won the Academy Award for Best Documentary Feature?", "Searching for Sugar Man", "20 Feet from Stardom", "Amy", "Sound City", "Searching for Sugar Man", "Searching for Sugar Man detailed two South African fans searching for their musical hero.", "Medium"),
    ("MOV161", "movies", "Which Italian director won the Academy Award for Best Foreign Language Film for 'Life Is Beautiful' (1997)?", "Roberto Benigni", "Giuseppe Tornatore", "Paolo Sorrentino", "Bernardo Bertolucci", "Roberto Benigni", "Roberto Benigni famously jumped over seats when receiving his Oscar.", "Medium"),
    ("MOV162", "movies", "What lens design compresses a wide horizontal field of view onto standard film frames, creating wide cinematic aspect ratios?", "Anamorphic Lens", "Spherical Lens", "Telephoto Lens", "Macro Lens", "Anamorphic Lens", "Anamorphic lenses produce oval bokeh reflections and characteristic lens flares.", "Medium"),
    ("MOV163", "movies", "Which screenwriter won three Academy Awards for Best Screenplay for 'Network', 'Marty', and 'The Hospital'?", "Paddy Chayefsky", "William Goldman", "Woody Allen", "Bo Goldman", "Paddy Chayefsky", "Paddy Chayefsky is the only writer to win three solo Academy Awards for screenplay.", "Medium"),
    ("MOV164", "movies", "Which 1988 live-action/animated comedy film co-produced by Steven Spielberg seamlessly combined hand-drawn animation with live actors?", "Who Framed Roger Rabbit", "Space Jam", "Cool World", "Enchanted", "Who Framed Roger Rabbit", "Who Framed Roger Rabbit won 3 Academy Awards for revolutionary technical achievements.", "Medium"),
    ("MOV165", "movies", "Who composed the memorable jazz-infused score for the 1963 film 'The Pink Panther' including its famous theme?", "Henry Mancini", "Lalo Schifrin", "Dave Brubeck", "Quincy Jones", "Henry Mancini", "Henry Mancini won four Academy Awards and 20 Grammy Awards in his career.", "Medium"),
    ("MOV166", "movies", "Which 2014 Edward Snowden surveillance documentary directed by Laura Poitras won the Academy Award for Best Documentary Feature?", "Citizenfour", "The Square", "Dirty Wars", "Finding Vivian Maier", "Citizenfour", "Citizenfour captured real-time secret interviews with whistle-blower Edward Snowden.", "Medium"),
    ("MOV167", "movies", "Which legendary film sound designer created the iconic lightsaber ignition, TIE fighter scream, and Darth Vader breathing sounds?", "Ben Burtt", "Gary Rydstrom", "Walter Murch", "Skip Lievsay", "Ben Burtt", "Ben Burtt was awarded a Special Achievement Academy Award for Star Wars sound.", "Medium"),
    ("MOV168", "movies", "Which Iranian film director directed 'A Separation' (2011) and 'The Salesman' (2016), both winning Oscars for Best Foreign Language Film?", "Asghar Farhadi", "Abbas Kiarostami", "Jafar Panahi", "Majid Majidi", "Asghar Farhadi", "Asghar Farhadi is one of few directors to win two Foreign Language Film Oscars.", "Medium"),
    ("MOV169", "movies", "What is the process of adjusting and enhancing colors in recorded film footage during post-production called?", "Color Grading", "Color Keying", "Compositing", "Focus Stacking", "Color Grading", "Color grading establishes emotional tone and visual continuity across scenes.", "Medium"),
    ("MOV170", "movies", "Which screenwriter wrote the screenplays for classic films 'Butch Cassidy and the Sundance Kid' (1969) and 'All the President's Men' (1976)?", "William Goldman", "Paul Schrader", "Ernest Lehman", "Ben Hecht", "William Goldman", "William Goldman famously wrote the Hollywood book 'Adventures in the Screen Trade'.", "Medium"),
    ("MOV171", "movies", "Which 2015 British documentary about singer Amy Winehouse directed by Asif Kapadia won the Academy Award for Best Documentary Feature?", "Amy", "Listen to Me Marlon", "What Happened, Miss Simone?", "Winter on Fire", "Amy", "Amy used archival footage to present the tragedy of Amy Winehouse.", "Medium"),
    ("MOV172", "movies", "Who was the legendary sound editor and film editor who won double Oscars for editing and sound mixing on 'English Patient' (1996)?", "Walter Murch", "Ben Burtt", "Gary Rydstrom", "Michael Kahn", "Walter Murch", "Walter Murch coined the term 'Sound Designer' while working on Apocalypse Now.", "Medium"),
    ("MOV173", "movies", "Which film director won the Palme d'Or twice at Cannes for 'The Wind That Shakes the Barley' (2006) and 'I, Daniel Blake' (2016)?", "Ken Loach", "Michael Haneke", "Ruben Östlund", "Jean-Pierre Dardenne", "Ken Loach", "British director Ken Loach is famous for social realism in cinema.", "Medium"),
    ("MOV174", "movies", "Which 1993 Steven Spielberg movie introduced photorealistic CGI dinosaurs created by ILM and Stan Winston's animatronics?", "Jurassic Park", "The Lost World", "Westworld", "Tremors", "Jurassic Park", "Jurassic Park's visual effects revolutionized Hollywood's reliance on CGI.", "Medium"),
    ("MOV175", "movies", "Which Taiwanese director directed the groundbreaking 2000 martial arts film 'Crouching Tiger, Hidden Dragon'?", "Ang Lee", "Zhang Yimou", "Wong Kar-wai", "Chen Kaige", "Ang Lee", "Crouching Tiger, Hidden Dragon won 4 Oscars including Best Foreign Language Film.", "Medium"),
    ("MOV176", "movies", "Which composer wrote the thrilling theme music for the 'Mission: Impossible' television series and films?", "Lalo Schifrin", "Jerry Goldsmith", "Lorne Balfe", "Danny Elfman", "Lalo Schifrin", "Lalo Schifrin's 5/4 time signature theme is an iconic spy motif.", "Medium"),
    ("MOV177", "movies", "What camera movement rotates the camera head horizontally left or right from a fixed position?", "Pan", "Tilt", "Tracking Shot", "Crane Shot", "Pan", "Panning rotates the camera on its vertical axis without moving its physical base.", "Medium"),
    ("MOV178", "movies", "Which 2019 documentary about female factory workers in Ohio won the Oscar, produced by Barack and Michelle Obama's Higher Ground?", "American Factory", "Honeyland", "The Edge of Democracy", "For Sama", "American Factory", "American Factory was Higher Ground Productions' debut release.", "Medium"),
    ("MOV179", "movies", "Who served as Steven Spielberg's primary film editor for over 40 years, editing films from 'Close Encounters' to 'Schindler's List'?", "Michael Kahn", "Thelma Schoonmaker", "Walter Murch", "Joe Hutshing", "Michael Kahn", "Michael Kahn has won three Academy Awards editing Spielberg's films.", "Medium"),
    ("MOV180", "movies", "Which Japanese master of atmosphere directed iconic Hong Kong films like 'In the Mood for Love' (2000) and 'Chungking Express' (1994)?", "Wong Kar-wai", "Edward Yang", "Hou Hsiao-hsien", "Tsui Hark", "Wong Kar-wai", "Wong Kar-wai is celebrated for vivid color palettes and lush romantic nostalgia.", "Medium"),

    # HARD (MOV181 - MOV200)
    ("MOV181", "movies", "Who was the pioneering cinematographer who shot Orson Welles' 'Citizen Kane' (1941) utilizing revolutionary deep focus photography?", "Gregg Toland", "Karl Struss", "Freddie Young", "James Wong Howe", "Gregg Toland", "Gregg Toland shared title billing with Orson Welles for his groundbreaking camera techniques.", "Hard"),
    ("MOV182", "movies", "Which 1928 French silent film directed by Carl Theodor Dreyer is acclaimed as a supreme masterpiece of close-up cinematography?", "The Passion of Joan of Arc", "Vampyr", "Un Chien Andalou", "Metropolis", "The Passion of Joan of Arc", "Renée Jeanne Falconetti's emotional close-ups remain legendary in film history.", "Hard"),
    ("MOV183", "movies", "Who is the only film sound mixer/designer in history to win 7 Academy Awards, including for 'Jurassic Park' and 'Saving Private Ryan'?", "Gary Rydstrom", "Ben Burtt", "Walter Murch", "Christopher Boyes", "Gary Rydstrom", "Gary Rydstrom won 7 Oscars across sound editing and sound mixing categories.", "Hard"),
    ("MOV184", "movies", "Which Austrian filmmaker won the Palme d'Or twice at Cannes for 'The White Ribbon' (2009) and 'Amour' (2012)?", "Michael Haneke", "Ruben Östlund", "Lars von Trier", "Yorgos Lanthimos", "Michael Haneke", "Michael Haneke is known for rigorous psychological analysis of modern European society.", "Hard"),
    ("MOV185", "movies", "Which legendary Hollywood screenwriter wrote the screenplays for 'Sunset Boulevard' (1950) and 'Some Like It Hot' (1959) alongside Billy Wilder?", "I. A. L. Diamond", "Charles Brackett", "Ben Hecht", "Herman J. Mankiewicz", "Charles Brackett", "Charles Brackett co-wrote classic Billy Wilder dramas before I.A.L. Diamond.", "Hard"),
    ("MOV186", "movies", "Which 1962 British epic historical film directed by David Lean won 7 Oscars with magnificent desert cinematography by Freddie Young?", "Lawrence of Arabia", "Doctor Zhivago", "The Bridge on the River Kwai", "A Passage to India", "Lawrence of Arabia", "Freddie Young used custom Super Panavision 70 lenses to capture desert mirages.", "Hard"),
    ("MOV187", "movies", "Which Swedish auteur filmmaker directed cinematic masterpieces 'The Seventh Seal' (1957) and 'Wild Strawberries' (1957)?", "Ingmar Bergman", "Victor Sjöström", "Bo Widerberg", "Roy Andersson", "Ingmar Bergman", "Ingmar Bergman's films explored existential philosophy, mortality, and human psychology.", "Hard"),
    ("MOV188", "movies", "Who co-wrote the screenplay for 'Citizen Kane' (1941) alongside Orson Welles, winning the Academy Award for Best Original Screenplay?", "Herman J. Mankiewicz", "Ben Hecht", "Robert Towne", "Dudley Nichols", "Herman J. Mankiewicz", "Herman 'Mank' Mankiewicz was the subject of David Fincher's 2020 biopic Mank.", "Hard"),
    ("MOV189", "movies", "What camera optical effect occurs when light reflects inside internal camera lens elements creating bright artifact shapes?", "Lens Flare", "Chromatic Aberration", "Vignetting", "Motion Blur", "Lens Flare", "Directors like J. J. Abrams frequently use stylistic lens flares in sci-fi films.", "Hard"),
    ("MOV190", "movies", "Which Russian film theorist and director pioneered the concept of montage editing in classic films like 'Battleship Potemkin' (1925)?", "Sergei Eisenstein", "Dziga Vertov", "Andrei Tarkovsky", "Lev Kuleshov", "Sergei Eisenstein", "Sergei Eisenstein demonstrated how juxtaposing shots creates psychological impact.", "Hard"),
    ("MOV191", "movies", "Which Greek director won the Golden Lion at Venice in 2023 for 'Poor Things' and received Palme d'Or nomination for 'The Lobster'?", "Yorgos Lanthimos", "Theo Angelopoulos", "Costa-Gavras", "Michael Cacoyannis", "Yorgos Lanthimos", "Yorgos Lanthimos is a leading figure of the Greek Weird Wave cinema movement.", "Hard"),
    ("MOV192", "movies", "Who was the legendary Polish film director who received an Honorary Oscar in 2000, famous for 'Three Colors' trilogy and 'Decalogue'?", "Krzysztof Kieślowski", "Andrzej Wajda", "Roman Polanski", "Pawel Pawlikowski", "Andrzej Wajda", "Andrzej Wajda was a central pillar of the Polish Film School movement.", "Hard"),
    ("MOV193", "movies", "Which 1927 Fritz Lang German expressionist sci-fi film featured futuristic cityscape visual effects that influenced modern cinema?", "Metropolis", "The Cabinet of Dr. Caligari", "M", "Nosferatu", "Metropolis", "Metropolis used the Schüfftan process to combine miniature models with live actors.", "Hard"),
    ("MOV194", "movies", "Which Italian cinematographer won three Academy Awards for Best Cinematography for 'Apocalypse Now', 'Reds', and 'The Last Emperor'?", "Vittorio Storaro", "Carlo Di Palma", "Tonino Delli Colli", "Gianni Di Venanzo", "Vittorio Storaro", "Vittorio Storaro is famous for his philosophical theory of color symbolism in film.", "Hard"),
    ("MOV195", "movies", "What is the psychological film editing phenomenon called where viewers derive more meaning from two sequential shots than a single shot?", "Kuleshov Effect", "Eisenstein Principle", "Bazin Realism", "Sontag Illusion", "Kuleshov Effect", "Lev Kuleshov showed that audience perception changes based on shot context.", "Hard"),
    ("MOV196", "movies", "Which American screenwriter holds the record for the most total Academy Award nominations for Best Screenplay (16 nominations)?", "Woody Allen", "Billy Wilder", "Quentin Tarantino", "Charles Brackett", "Woody Allen", "Woody Allen won three Best Original Screenplay Oscars among his 16 nominations.", "Hard"),
    ("MOV197", "movies", "Which Japanese stop-motion and visual effects artist co-created the legendary monster effects for 'Godzilla' (1954)?", "Eiji Tsuburaya", "Ishirō Honda", "Tomoyuki Tanaka", "Akira Ifukube", "Eiji Tsuburaya", "Eiji Tsuburaya introduced suitmation techniques to giant monster cinema.", "Hard"),
    ("MOV198", "movies", "Which 1968 Soviet epic drama directed by Sergei Bondarchuk won the Oscar for Best Foreign Language Film and lasted over 7 hours?", "War and Peace", "Solaris", "Andrei Rublev", "The Cranes Are Flying", "War and Peace", "War and Peace utilized tens of thousands of Soviet soldiers as extras in battle scenes.", "Hard"),
    ("MOV199", "movies", "Who was the pioneering female film editor who cut classic Hollywood movies like 'Lawrence of Arabia' (1962) and 'Out of Africa' (1985)?", "Anne V. Coates", "Dede Allen", "Verna Fields", "Margaret Booth", "Anne V. Coates", "Anne V. Coates won the Oscar for editing Lawrence of Arabia's match-cut sequence.", "Hard"),
    ("MOV200", "movies", "Which Swedish cinematographer collaborated closely with director Ingmar Bergman on over 20 films including 'Persona' and 'Cries and Whispers'?", "Sven Nykvist", "Gunnar Fischer", "Hoyte van Hoytema", "Jörgen Persson", "Sven Nykvist", "Sven Nykvist won two Academy Awards for Best Cinematography working with Bergman.", "Hard")
]

print(f"Total Movies Batch 2 Questions: {len(movies_batch2)}")
assert len(movies_batch2) == 100, f"Expected 100, got {len(movies_batch2)}"

# Check IDs
ids = [q[0] for q in movies_batch2]
assert len(set(ids)) == 100, "Duplicate IDs in Movies Batch 2!"
assert ids[0] == "MOV101" and ids[-1] == "MOV200", f"ID bounds mismatch: {ids[0]} to {ids[-1]}"

# Check difficulties
easies = [q for q in movies_batch2 if q[9] == 'Easy']
mediums = [q for q in movies_batch2 if q[9] == 'Medium']
hards = [q for q in movies_batch2 if q[9] == 'Hard']

print(f"Movies Batch 2 Breakdown -> Easy: {len(easies)} (MOV101-MOV140), Medium: {len(mediums)} (MOV141-MOV180), Hard: {len(hards)} (MOV181-MOV200)")
assert len(easies) == 40
assert len(mediums) == 40
assert len(hards) == 20

# Check duplicate options and correct answer existence
for q in movies_batch2:
    q_id = q[0]
    opts = [q[3], q[4], q[5], q[6]]
    ans = q[7]
    assert len(set(opts)) == 4, f"Duplicate options in {q_id}: {opts}"
    assert ans in opts, f"Correct answer '{ans}' not in options for {q_id}: {opts}"

# Check uniqueness of question text across Batch 2
q_texts = [q[2] for q in movies_batch2]
assert len(set(q_texts)) == 100, "Duplicate question text in Movies Batch 2!"

# Verify against MOV001-MOV100
with open('app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt') as f:
    existing_kt = f.read()

start_b1 = existing_kt.find('private val moviesSeeds = listOf(')
end_b1 = existing_kt.find('private val technologySeeds = listOf(', start_b1)
movies_b1_block = existing_kt[start_b1:end_b1]

pattern = re.compile(r'QuestionEntity\(\"([^\"]+)\",\s*\"([^\"]+)\",\s*\"([^\"]+)\"')
b1_matches = pattern.findall(movies_b1_block)
b1_questions = set(m[2].lower() for m in b1_matches)

for q in movies_batch2:
    q_txt = q[2].lower()
    assert q_txt not in b1_questions, f"Question '{q[2]}' in Batch 2 collides with Batch 1!"

print("Batch 1 vs Batch 2 collision check passed 100% cleanly!")

# Format Kotlin statements
kotlin_lines = []
for q in movies_batch2:
    q_id, cat, text, opA, opB, opC, opD, ans, exp, diff = q
    text_e = text.replace('"', '\\"')
    opA_e = opA.replace('"', '\\"')
    opB_e = opB.replace('"', '\\"')
    opC_e = opC.replace('"', '\\"')
    opD_e = opD.replace('"', '\\"')
    ans_e = ans.replace('"', '\\"')
    exp_e = exp.replace('"', '\\"')
    line = f'        QuestionEntity("{q_id}", "{cat}", "{text_e}", "{opA_e}", "{opB_e}", "{opC_e}", "{opD_e}", "{ans_e}", "{exp_e}", "{diff}")'
    kotlin_lines.append(line)

b2_kotlin_str = ",\n".join(kotlin_lines)

# Find the end of MOV100 inside moviesSeeds in DefaultQuestionSeeds.kt
# MOV100 is currently the last item in moviesSeeds list
mov100_idx = existing_kt.find('QuestionEntity("MOV100"')
if mov100_idx == -1:
    sys.exit("Could not find MOV100 in DefaultQuestionSeeds.kt!")

closing_paren = existing_kt.find(')', mov100_idx)
if closing_paren == -1:
    sys.exit("Could not find closing parenthesis after MOV100!")

# Insert MOV101-MOV200 before closing parenthesis
updated_kt = existing_kt[:closing_paren].rstrip() + ",\n" + b2_kotlin_str + "\n    " + existing_kt[closing_paren:]

with open('app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt', 'w') as f:
    f.write(updated_kt)

print("Successfully appended MOV101-MOV200 into moviesSeeds in DefaultQuestionSeeds.kt!")
