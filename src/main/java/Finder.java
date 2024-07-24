import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Finder{
    final static int DEFAULT_MIN_WORD_LENGTH = 3;
    final static int DEFAULT_MAX_WORD_LENGTH = 8;
    int min_word_length = DEFAULT_MIN_WORD_LENGTH;
    int max_word_length = DEFAULT_MAX_WORD_LENGTH;

    URL file = Finder.class.getClassLoader().getResource("en.dict");
    Scanner dictionary = new Scanner(Path.of(file.toURI()));

    public Finder() throws URISyntaxException, IOException {
    }

    public static void main( String... args ) throws IOException, URISyntaxException {
        new Finder().init( args );
    }

    public void init( String... args ) throws FileNotFoundException {
        Finder.class.getClassLoader().getResourceAsStream("en.dict");

        if( args.length == 0 ){
            printUsage();
            return;
        } else if( args.length > 3 )
            err("Too many args.");

        // Only if arg[2] isn't set; which is the new max_length arg
        if( max_word_length > args[0].length() && args.length < 2 )
            err("Error: Cannot find words longer than the number of available letters.");

        System.out.println("Case: " + args[0]);
        switch( args[0] ){
            case "add":
                if( args.length < 2 ){
                    printUsage();
                } else {
                    System.out.println("Add To Dict");
                    addToDict(args[1]);
                }
                return;
            case "remove":
                if( args.length < 2 ){
                    printUsage();
                } else {
                    removeFromDict(args[1]);
                }
                return;
            default:
                if( !args[0].matches("^[a-z]+$") )
                    err("Input string can *only* contain letters.");

                if( args.length > 1 ){
                    try{
                        min_word_length = max_word_length = Integer.parseInt( args[1] );
                        if( min_word_length <= 0 )
                            err("Invalid minimum word length.");

                        if( args.length > 1 ) {
                            try {
                                max_word_length = Integer.parseInt(args[2]);
                                if( max_word_length > args[0].length() )
                                    err("Cannot find words longer than the number of available letters.");
                                if( max_word_length < min_word_length )
                                    err( "Maximum length cannot be less than minimum length." );
                            } catch (NumberFormatException e) {
                                err("'%s' isn't a number.", args[2]);
                            }
                        }

                        // Find words within the defined length range
                        findAllWords( args[0] );
                        return;
                    } catch( NumberFormatException e ){
                        // Second arg isn't a number; assume it's a constrained word
                        if( args[0].length() < args[1].length() )
                            err("The number of available letters must be geq than the size of the constrained word.");

                        findConstrainedWords( args[0], args[1] );
                        return;
                    }
                } else {
                    if( args[0].length() < min_word_length )
                        min_word_length = args.length;
                    if( args[0].length() > max_word_length )
                        max_word_length = args.length;
                    findAllWords( args[0] );

                    return;
                }
        }
    }

    public static void err( String... msg ){
        System.out.println( "Error: " + msg );
        System.exit(1);
    }

    public void printUsage(){
        System.out.println(
                "Usage:\n" +
                        "[LETTERS] {int, {int}}\t\tThe letters used to find words (do not separate with spaces)\n" +
                        "                      \t\tOptionally, if a number is provided, only words of that length will be found; two numbers for a range\n" +
                        "[LETTERS] [____]      \t\tFind words with letters at specific places within a word; use underscores for open letters\n" +
                        "ADD [word]            \t\tAdds the word to the dictionary\n" +
                        "REMOVE [word}         \t\tRemoves the word from the dictionary\n\n"
        );
    }

    public boolean addToDict( String word ){
        boolean successful = false;
        if( word == null || word.isEmpty() )
            return successful;

        File inputFile = new File("en.dict");
        File tempFile = new File("tmp.dict");

        try(
            BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        ) {
            System.out.println("Writing to file: " + file.getFile());
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                int comp = trimmedLine.compareToIgnoreCase(word);
                if( comp == 0 )
                    writer.write(currentLine + System.getProperty("line.separator"));
                else if( comp == -1 ){
                    writer.write(word + System.getProperty("line.separator"));
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
            System.out.println("Renaming: " + file.getFile() + "\nTo: " + tempFile.getAbsolutePath());
            successful = tempFile.renameTo(inputFile);
        } catch( Exception e ){
            e.printStackTrace();
            successful = false;
            tempFile.delete();
        } finally {
            System.out.println("Success? " + successful);
            return successful;
        }
    }

    public boolean removeFromDict( String word ){
        boolean successful = false;
        if( word == null || word.isEmpty() )
            return successful;

        File inputFile = new File("en.dict");
        File tempFile = new File("tmp.dict");

        try(
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        ) {
            String currentLine;
            while( (currentLine = reader.readLine()) != null ){
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                if( trimmedLine.equalsIgnoreCase(word) ) continue;
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            successful = tempFile.renameTo(inputFile);
        } catch( Exception e ){
            e.printStackTrace();
            successful = false;
            tempFile.delete();
        } finally {
            return successful;
        }
    }

    public int findAllWords( String letters ) { // Within length range
        return findAllWords(
            letters.chars().mapToObj(c -> (char) c).collect(Collectors.toList())
        );
    }

    // Returns number of words found
    public int findAllWords( List<Character> letters ){ // Within length range
//        Scanner dictionary = new Scanner( new File("src/main/resources/en.dict") );
        int wordsFound = 0;
        while (dictionary.hasNextLine()) {
            char[] word = dictionary.nextLine().toCharArray();
            if( word.length < min_word_length || word.length > max_word_length )
                continue;

            List<Character> _letters = new LinkedList<>();
            _letters.addAll(letters);

            boolean wordFound = true;
            for( char ch : word ){
                if( !_letters.remove((Character)ch) ){
                    // A letter was found in the word that's not in the letter list; skip this dict word
                    wordFound = false;
                    break;
                }
            }
            if( wordFound ) {
                System.out.println(new String(word));
                wordsFound++;
            }
        }

        if( wordsFound == 0 )
            System.out.println("Nothing found matching the supplied criteria.");

        return wordsFound;
    }

    public int findConstrainedWords( String letters, String partialWord ) { // Within length range
        return findConstrainedWords(
            letters.chars().mapToObj(c -> (char) c).collect(Collectors.toList()),
            partialWord.toCharArray()
        );
    }

    public int findConstrainedWords( List<Character> letters, char[] partialWord ){ // Within length range
//        Scanner dictionary = new Scanner( new File("src/main/resources/en.dict") );
        int wordsFound = 0;
        while (dictionary.hasNextLine()) {
            char[] word = dictionary.next().toCharArray();
            if( partialWord.length != word.length )
                continue;

            List<Character> _letters = new LinkedList<>();
            _letters.addAll(letters);

            boolean wordFound = true;
            for( int i = 0; i < partialWord.length; i++ ){
                char ch = word[i];
                if( !(( partialWord[i] == '_' || partialWord[i] == ch ) && _letters.remove((Character) ch) ) ){
                    // A letter was found in the word that's not in the letter list; skip this dict word
                    wordFound = false;
                    break;
                }
            }
            if( wordFound ) {
                System.out.println(new String(word));
                wordsFound++;
            }
        }

        if( wordsFound == 0 )
            System.out.println("Nothing found matching the supplied criteria.");

        return wordsFound;
    }
}