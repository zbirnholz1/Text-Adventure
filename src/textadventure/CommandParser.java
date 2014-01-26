package textadventure;
import java.io.*;
import java.util.*;

public class CommandParser {
	private String verb, objectName, indirectObjectName, lastVerb, objectAdjective, indirectObjectAdjective, lastObjectName, lastIndirectObjectName;
	private TAObject object, indirectObject, lastObject, lastIndirectObject;
	private Map<String, List<String>> commands;
	private Map<String, List<String>> keywords;
	private Map<String, String> IOquestionWords;
	private Map<String, String> IOprepositions;
	private boolean lookingJustForObject, justLookedJustForObject, lookingJustForObjectAdjective, justLookedJustForObjectAdjective, lookingJustForIO, lookingJustForIOAdjective, justLookedJustForIO, justLookedJustForIOAdjective; //if the player wasn't specific enough with the last command
	private boolean objectIsVague, indirectObjectIsVague;
	private boolean hasMultipleWords;

	public static final Set<Character> SPECIAL_PUNCTUATION=new HashSet<Character>(Arrays.asList('!', ' ', '_', '#', '&', ':', '-', '[', ']', '"', '^'));
	//add to SPECIAL_PUNCTUATION as necessary
	public static Set<String> NON_MOVE_ADDING_VERBS;
	//NON_MOVE_ADDING_VERBS can't be final, but should not be changed outside of the constructor!

	public CommandParser() {
		NON_MOVE_ADDING_VERBS=new HashSet<String>();
		commands=new HashMap<String, List<String>>();
		keywords=new HashMap<String, List<String>>();
		IOquestionWords=new HashMap<String, String>();
		IOprepositions=new HashMap<String, String>();
		lastVerb=null;
		lastObject=null;
		lastIndirectObject=null;
		lookingJustForObject=false;
		justLookedJustForObject=false;
		lookingJustForObjectAdjective=false;
		justLookedJustForObjectAdjective=false;
		lookingJustForIO=false;
		justLookedJustForIO=false;
		lookingJustForIOAdjective=false;
		justLookedJustForIOAdjective=false;
		hasMultipleWords=false;
		try {
			String path="/commands.taf";
			//if(new File("rsc").exists())
			//path="rsc/"+path;
			BufferedReader reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));
			String line=reader.readLine();
			while(line.charAt(0)=='#')
				line=reader.readLine();
			while(line!=null) {
				String[] commandsAndKeywords=line.split(":");
				String toAdd=line.substring(0, line.indexOf(":"));
				if(!commandsAndKeywords[1].equals(" ")) {
					StringTokenizer stringOfSynonyms=new StringTokenizer(commandsAndKeywords[1], ",");
					List<String> listOfSynonyms=new LinkedList<String>();
					while(stringOfSynonyms.hasMoreTokens())
						listOfSynonyms.add(stringOfSynonyms.nextToken().trim());
					if(toAdd.charAt(0)=='=') {
						toAdd=toAdd.substring(1);
						NON_MOVE_ADDING_VERBS.add(toAdd);
					}
					commands.put(toAdd, listOfSynonyms);
				}
				else {
					if(toAdd.charAt(0)=='=') {
						toAdd=toAdd.substring(1);
						NON_MOVE_ADDING_VERBS.add(toAdd);
					}
					commands.put(toAdd, new LinkedList<String>());
				}
				if(!commandsAndKeywords[2].equals(" ")) {
					StringTokenizer stringOfKeywords=new StringTokenizer(commandsAndKeywords[2], ",");
					List<String> listOfKeywords=new LinkedList<String>();
					while(stringOfKeywords.hasMoreTokens())
						listOfKeywords.add(stringOfKeywords.nextToken().trim());
					keywords.put(toAdd, listOfKeywords);
				}
				else
					keywords.put(toAdd, new LinkedList<String>());
				if(commandsAndKeywords.length>3)
					IOquestionWords.put(toAdd, commandsAndKeywords[3]);
				else
					IOquestionWords.put(toAdd, "What");
				if(commandsAndKeywords.length>4)
					IOprepositions.put(toAdd, commandsAndKeywords[4]);
				else
					IOprepositions.put(toAdd, "with");
				line=reader.readLine();
			}
			reader.close();
		} catch(IOException e){Main.game.getView().println("Something went wrong: "+e);}
	}

	public void parse(String str) {
		parse(str, false);
	}

	public void parse(String str, boolean isUserInput) {
		lastVerb=verb;
		lastObject=object;
		lastIndirectObject=indirectObject;
		lastObjectName=objectName;
		lastIndirectObject=indirectObject;
		lastIndirectObjectName=indirectObjectName;
		verb=null;
		objectName=null;
		object=null;
		indirectObjectName=null;
		indirectObject=null;
		objectAdjective=null;
		indirectObjectAdjective=null;
		objectIsVague=false;
		indirectObjectIsVague=false;
		if(str.length()>0&&str.charAt(0)!='_') {
			str=str.toLowerCase();
			//str=removeExtraCharacters(str);
			//It does that^ later.
		}
		if(str.equals("")) {
			Main.game.getView().println(blankResponse());
			lookingJustForObject=false;
			lookingJustForObjectAdjective=false;
			return;
		}
		String[] words=str.split(" ");
		hasMultipleWords=words.length>1;
		int startIndex=1;
		if(words.length>1) 
			verb=verbSynonym(words[0]+" "+words[1]);
		if(verb==null) {
			if(commands.containsKey(words[0])) {
				verb=words[0];
				lookingJustForObject=false;
				lookingJustForObjectAdjective=false;
			}
			else
				verb=verbSynonym(words[0]);
			if(verb==null&&!lookingJustForObject&&!lookingJustForObjectAdjective&&!lookingJustForIO&&!lookingJustForIOAdjective) {
				Main.game.getView().println("I don't know how to "+words[0]+".");
				return;
			}
			else if(verb!=null) {
				lookingJustForObject=false;
				lookingJustForObjectAdjective=false;
				lookingJustForIO=false;
				lookingJustForIOAdjective=false;
			}
		}
		else {
			startIndex=2;
			lookingJustForObject=false;
			lookingJustForObjectAdjective=false;
			lookingJustForIO=false;
			lookingJustForIOAdjective=false;
		}
		if(keywords.containsKey(verb)&&!keywords.get(verb).contains("\"")&&str.charAt(0)!='_') {
			str=removeExtraCharacters(str);
			words=str.split(" ");
		}
		if(lookingJustForObject) {
			verb=lastVerb;
			if(lastObject!=null) {
				objectName=lastObject.getName();
				object=lastObject;
			}
		}
		else if(lookingJustForObjectAdjective) {
			verb=lastVerb;
			if(lastObject!=null) {
				object=lastObject;
				objectName=lastObject.getName();
				objectAdjective=lastObject.getAdjective();
			}
			else if(lastObjectName!=null)
				objectName=lastObjectName;
			if(lastIndirectObject!=null) {
				indirectObject=lastIndirectObject;
				indirectObjectName=lastIndirectObject.getName();
				indirectObjectAdjective=lastIndirectObject.getAdjective();
			}
			else if(lastIndirectObjectName!=null)
				indirectObjectName=lastIndirectObjectName;
		}
		else if(lookingJustForIO) {
			verb=lastVerb;
			object=lastObject;
			objectName=lastObject.getName();
			objectAdjective=lastObject.getAdjective();
			if(lastIndirectObject!=null) {
				indirectObjectName=lastIndirectObject.getName();
				indirectObjectAdjective=lastIndirectObject.getAdjective();
			}
		}
		else if(lookingJustForObjectAdjective) {
			verb=lastVerb;
			object=lastObject;
			objectName=lastObject.getName();
			objectAdjective=lastObject.getAdjective();
			if(lastIndirectObject!=null) {
				indirectObject=lastIndirectObject;
				indirectObjectName=lastIndirectObject.getName();
				indirectObjectAdjective=lastIndirectObject.getAdjective();
			}
			else if(lastIndirectObjectName!=null)
				indirectObjectName=lastIndirectObjectName;
		}
		if(lookingJustForObject||lookingJustForObjectAdjective||lookingJustForIO||lookingJustForIOAdjective)
			startIndex=0;
		if(isUserInput&&verb.charAt(0)=='_') {
			if(verb.length()>1)
				Main.game.getView().println("I don't know how to "+verb);
			else
				Main.game.getView().println(blankResponse());
			return;
		}
		boolean quoteIsOpen=false;
		boolean finishedWithObject=false;
		for(int i=startIndex; i<words.length; i++) {
			String word=words[i];
			if(lookingJustForObjectAdjective&&objectName!=null) {
				if(objectAdjective==null) {
					objectAdjective=words[i];
					object=Main.game.getPlayer().getRoom().getObject(objectName, objectAdjective);
					continue;
				}
				else if(indirectObjectAdjective==null&&indirectObjectName!=null) {
					indirectObjectAdjective=words[i];
					indirectObject=Main.game.getPlayer().getRoom().getObject(indirectObjectName, indirectObjectAdjective);
					break;
				}
				else
					break;
			}
			if(lookingJustForIOAdjective&&indirectObjectName!=null) {
				if(indirectObjectAdjective==null) {
					indirectObjectAdjective=words[i];
					indirectObject=Main.game.getPlayer().getRoom().getObject(indirectObjectName, indirectObjectAdjective);
					continue;
				}
				else
					break;
			}
			if(word.charAt(0)=='\"') {
				quoteIsOpen=true;
				word=word.substring(1);
			}
			boolean nextQuoteIsOpen=quoteIsOpen;
			if(word.length()>0&&word.charAt(word.length()-1)=='\"') {
				nextQuoteIsOpen=false;
				word=word.substring(0, word.length()-1);
			}
			if(objectName==null) {
				if(keywords.get(verb)!=null&&(keywords.get(verb).contains(word)||keywords.get(verb).contains("*"))) {
					objectName=word;
					if(!quoteIsOpen)
						finishedWithObject=true;
				}
				else if(Main.game.getPlayer().getRoom().contains(word, verb.charAt(0)=='_')
						||Main.game.getPlayer().has(word)
						||Room.DIRECTIONS.contains(word))
					objectName=word;
				if(objectName!=null) {
					if(i>0)
						objectAdjective=words[i-1];
					if(objectAdjective==null||objectAdjective.equals(verb)||commands.get(verb).contains(objectAdjective)) {
						objectAdjective=null;
						objectIsVague=Main.game.getPlayer().getRoom().containsMultiple(objectName)&&Main.game.getPlayer().getRoom().getObject(objectName, objectAdjective)==null;
					}
				}
			}
			else if(indirectObjectName==null) {
				if(quoteIsOpen&&!finishedWithObject)
					objectName+=" "+word;
				else if(keywords.get(verb)!=null&&(keywords.get(verb).contains(word)||keywords.get(verb).contains("*"))) {
					indirectObjectName=word;
				}
				else if(Main.game.getPlayer().getRoom().contains(word, verb.charAt(0)=='_')
						||Main.game.getPlayer().has(word)
						||Room.DIRECTIONS.contains(word)) {
					indirectObjectName=word;
				}
				if(indirectObjectName!=null) {
					if(i>0)
						indirectObjectAdjective=words[i-1];
					if(objectName.equals(indirectObjectAdjective)) {
						indirectObjectAdjective=null;
						indirectObjectIsVague=Main.game.getPlayer().getRoom().containsMultiple(indirectObjectName)&&Main.game.getPlayer().getRoom().getObject(indirectObjectName, indirectObjectAdjective)==null;
					}
					else
						break;
				}
			}
			else if(quoteIsOpen)
				indirectObjectName+=" "+word;
			if(quoteIsOpen&&!nextQuoteIsOpen)
				finishedWithObject=true;
			quoteIsOpen=nextQuoteIsOpen;
		}
		if(!objectIsVague) {
			if(objectName!=null&&!Main.game.getPlayer().getRoom().containsMultiple(objectName)) {
				object=Main.game.getPlayer().getRoom().getObject(objectName, objectAdjective);
				if(object==null)
					object=Main.game.getPlayer().getRoom().getObject(objectName);
			}
			if(Main.game.getPlayer().has(objectName)) {
				object=Main.game.getPlayer().get(objectName, objectAdjective);
				if(object==null)
					object=Main.game.getPlayer().get(objectName);
			}
		}
		if(!indirectObjectIsVague) {
			if(indirectObjectName!=null) //the player needs to have the IO
				indirectObject=Main.game.getPlayer().getRoom().getObject(indirectObjectName);
			if(Main.game.getPlayer().has(indirectObjectName))
				indirectObject=Main.game.getPlayer().get(indirectObjectName);
		}
		if(verb.charAt(0)!='_') {
			if(object==null&&words.length>1&&objectName==null)
				objectName=words[1];
			else if(object==null&&lookingJustForObject&&words.length>0)
				objectName=words[0];
			if(indirectObject==null&&indirectObjectName==null) {
				if(lookingJustForIO&&words.length>0)
					indirectObjectName=words[0];
				for(int i=2; i<words.length; i++) {
					if(!words[i].equals("with")&&!words[i].equals("to")&&!words[i].equals("about")) {
						indirectObjectName=words[i];
					}
				}
			}
		}
		if(keywords.get(verb).contains("\"")&&str.contains(" "))
			objectName=str.substring(str.indexOf(" ")+1);
		int potentialDirection=Main.game.getPlayer().getRoom().getEquivalentDirection(verb, object, indirectObject);
		if(potentialDirection==-1)
			potentialDirection=Main.game.getPlayer().getRoom().getEquivalentDirection(verb+" "+str.substring(str.indexOf(" ")+1));
		if(potentialDirection!=-1)
			verb=Room.DIRECTIONS.get(potentialDirection);
		justLookedJustForObject=lookingJustForObject;
		lookingJustForObject=false;
		justLookedJustForObjectAdjective=lookingJustForObjectAdjective;
		lookingJustForObjectAdjective=false;
		justLookedJustForIO=lookingJustForIO;
		lookingJustForIO=false;
		justLookedJustForIOAdjective=lookingJustForIOAdjective;
		lookingJustForIOAdjective=false;
		Main.game.processCommand(verb);
		//print what was parsed for debugging
		//Main.game.getView().println("verb: "+verb+"\nobject: "+objectAdjective+" "+objectName+"\nIO: "+indirectObjectAdjective+" "+indirectObjectName);
	}

	private String verbSynonym(String str) {
		for(String cmd:commands.keySet()) {
			for(String syn:commands.get(cmd)) {
				if(syn.equalsIgnoreCase(str))
					return cmd;
			}
		}
		return null;
	}

	private String removeExtraCharacters(String str) {
		//remove extra whitespace
		str=str.trim();
		while(str.contains("  "))
			str=str.replace("  ", " ");
		//remove unnecessary articles
		while(str.contains(" a "))
			str=str.replace(" a ", " ");
		while(str.contains(" an "))
			str=str.replace(" an ", " ");
		while(str.contains(" the "))
			str=str.replace(" the ", " ");
		if(str.length()>2&&str.substring(str.length()-2, str.length()).equals(" a"))
			str=str.substring(0, str.length()-2);
		else if(str.length()>3&&str.substring(str.length()-3, str.length()).equals(" an"))
			str=str.substring(0, str.length()-3);
		else if(str.length()>4&&str.substring(str.length()-4, str.length()).equals(" the"))
			str=str.substring(0, str.length()-4);
		if(str.length()>2&&str.substring(0, 2).equals("a "))
			str=str.substring(2);
		else if(str.length()>3&&str.substring(0, 3).equals("an "))
			str=str.substring(3);
		else if(str.length()>4&&str.substring(0, 4).equals("the "))
			str=str.substring(4);
		char[] originalChars=str.toCharArray();
		char[] newChars=new char[originalChars.length];
		//remove unnecessary punctuation
		for(int readIndex=0, writeIndex=0; readIndex<originalChars.length; readIndex++) {
			if((originalChars[readIndex]>='a'&&originalChars[readIndex]<='z')||(originalChars[readIndex]>='0'&&originalChars[readIndex]<='9')||CommandParser.SPECIAL_PUNCTUATION.contains(originalChars[readIndex])) {
				newChars[writeIndex]=originalChars[readIndex];
				writeIndex++;
			}
		}
		return new String(newChars).trim();
	}

	private String blankResponse() {
		int random=(int)(Math.random()*3);
		if(random==0)
			return "I beg your pardon?";
		else if(random==1)
			return "What?";
		else
			return "Please enter a command.";
	}

	public TAObject getObject() {
		return object;
	}

	public TAObject getIndirectObject() {
		return indirectObject;
	}

	public String getObjectName() {
		return objectName;
	}
	
	public String getObjectNameWithArticle() {
		String toReturn=null;
		if(objectName!=null) {
			if("aeiou".contains(""+objectName.charAt(0)))
				toReturn="an ";
			else
				toReturn="a ";
			toReturn+=objectName;
		}
		return toReturn;
	}

	public String getIndirectObjectName() {
		return indirectObjectName;
	}
	
	public String getIndirectObjectNameWithArticle() {
		String toReturn=null;
		if(indirectObjectName!=null) {
			if("aeiou".contains(""+indirectObjectName.charAt(0)))
				toReturn="an ";
			else
				toReturn="a ";
			toReturn+=indirectObjectName;
		}
		return toReturn;
	}

	public String getObjectAdjective() {
		return objectAdjective;
	}
	
	public String getObjectAdjectiveWithArticle() {
		String toReturn=null;
		if(objectAdjective!=null) {
			if("aeiou".contains(""+objectAdjective.charAt(0)))
				toReturn="an ";
			else
				toReturn="a ";
			toReturn+=objectAdjective;
		}
		return toReturn;
	}

	public String getIndirectObjectAdjective() {
		return indirectObjectAdjective;
	}
	
	public String getIndirectObjectAdjectiveWithArticle() {
		String toReturn=null;
		if(indirectObjectAdjective!=null) {
			if("aeiou".contains(""+indirectObjectAdjective.charAt(0)))
				toReturn="an ";
			else
				toReturn="a ";
			toReturn+=indirectObjectAdjective;
		}
		return toReturn;
	}

	public boolean isLookingJustForObject() {
		return lookingJustForObject;
	}

	public boolean isLookingJustForObjectAdjective() {
		return lookingJustForObjectAdjective;
	}

	public boolean justLookedJustForObject() {
		return justLookedJustForObject;
	}

	public boolean justLookedJustForObjectAdjective() {
		return justLookedJustForObjectAdjective;
	}

	public boolean justLookedJustForIO() {
		return justLookedJustForIO;
	}

	public boolean justLookedJustForIOAdjective() {
		return justLookedJustForIOAdjective;
	}

	public boolean objectIsVague() {
		return objectIsVague;
	}

	public boolean indirectObjectIsVague() {
		return indirectObjectIsVague;
	}

	public boolean hasMultipleWords() {
		return hasMultipleWords;
	}

	public void lookJustForObject() {
		lookingJustForObject=true;
	}

	public void lookJustForIndirectObject() {
		lookingJustForIO=true;
	}

	public void lookJustForObjectAdjective() {
		lookingJustForObjectAdjective=true;
	}

	public void lookJustForIndirectObjectAdjective() {
		lookingJustForIOAdjective=true;
	}

	public String getLastVerb() {
		return lastVerb;
	}

	public String getLastObjectName() {
		return lastObjectName;
	}

	public String getLastIndirectObjectName() {
		return lastIndirectObjectName;
	}

	public String getIOQuestionWord(String verb) {
		return IOquestionWords.get(verb);
	}

	public String getIOPreposition(String verb) {
		return IOprepositions.get(verb);
	}
	
	public TAObject getLastObject() {
		return lastObject;
	}
	
	public TAObject getLastIndirectObject() {
		return lastIndirectObject;
	}
}
