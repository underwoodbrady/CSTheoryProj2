import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
/**
 *
 * @author yaw
 */
public class RegularExpression {

    private String regularExpression;
    private NFA nfa;
    private int stateCounter = 0;


    // You are not allowed to change the name of this class or this constructor at all.
    public RegularExpression(String regularExpression) {
        this.regularExpression = regularExpression.replaceAll("\\s+", "");
        nfa = generateNFA();
    }

    // TODO: Complete this method so that it returns the nfa resulting from unioning the two input nfas.
    private NFA union(NFA nfa1, NFA nfa2) {
        // Create a unique set of states and transitions
        HashSet<String> newStates = new HashSet<>();
        HashMap<String, HashMap<Character, HashSet<String>>> newTransitions = new HashMap<>();

        // Helper to create unique state names and keep track of original states to new states mapping
        HashMap<String, String> stateUpdater1 = new HashMap<>();
        HashMap<String, String> stateUpdater2 = new HashMap<>();

        // Update state names to be unique and prepare transition mappings
        updateStateNames(nfa1, newStates, stateUpdater1, newTransitions);
        updateStateNames(nfa2, newStates, stateUpdater2, newTransitions);

        // Merge alphabets from both NFAs
        char[] mergedAlphabet = mergeAlphabets(nfa1.getAlphabet(), nfa2.getAlphabet());

        //combine transitions
        HashMap<String, HashMap<Character, HashSet<String>>> combinedTransitions = combineTransitions(nfa1, nfa2, stateUpdater1, stateUpdater2);

        
        // Create new start state and transitions from it using epsilon transitions to original start states
        String startState = "sUnion";
        newStates.add(startState);
        HashMap<Character, HashSet<String>> startTransitions = new HashMap<>();
        HashSet<String> transitionTo = new HashSet<String>();
        transitionTo.add(stateUpdater1.get(nfa1.getStartState()));
        transitionTo.add(stateUpdater2.get(nfa2.getStartState()));
        startTransitions.put('e', transitionTo);
        combinedTransitions.put(startState, startTransitions);

        // Combine accept states from both NFAs
        String[] newAcceptStates = combineAcceptStates(nfa1, nfa2, stateUpdater1, stateUpdater2);

        // Create the new NFA
        Object[] newStatesArrayO = newStates.toArray();
        String[] newStatesArray = new String[newStatesArrayO.length];
        int i = 0;
        for(Object o: newStatesArrayO) {
        	newStatesArray[i] = o.toString();
        	i++;
        }
     // Use combinedTransitions to create a new NFA or update an existing one
        NFA nfaU = new NFA(newStatesArray, mergedAlphabet, combinedTransitions, startState, newAcceptStates);
      
        return nfaU;
    }


    // Implement helper methods
    private HashMap<String, HashMap<Character, HashSet<String>>> combineTransitions(
    	    NFA nfa1, NFA nfa2,
    	    HashMap<String, String> stateUpdater1,
    	    HashMap<String, String> stateUpdater2) {

    	    // Create a new transitions map to return
    	    HashMap<String, HashMap<Character, HashSet<String>>> newTransitions = new HashMap<>();

    	    // Combine transitions from nfa1
    	    for (Map.Entry<String, HashMap<Character, HashSet<String>>> entry : nfa1.getTransitions().entrySet()) {
    	        String originalState = entry.getKey();
    	        HashMap<Character, HashSet<String>> transitions = entry.getValue();

    	        // Get the new state name for the original state from nfa1
    	        String newState = stateUpdater1.get(originalState);
    	        HashMap<Character, HashSet<String>> newTransStateMap = newTransitions.getOrDefault(newState, new HashMap<>());

    	        for (Map.Entry<Character, HashSet<String>> trans : transitions.entrySet()) {
    	            Character symbol = trans.getKey();
    	            HashSet<String> newTargetStates = new HashSet<>();

    	            // Update each target state to its new name
    	            for (String targetState : trans.getValue()) {
    	                newTargetStates.add(stateUpdater1.get(targetState));
    	            }

    	            // Merge transitions for the same character
    	            if (newTransStateMap.containsKey(symbol)) {
    	                newTransStateMap.get(symbol).addAll(newTargetStates);
    	            } else {
    	                newTransStateMap.put(symbol, newTargetStates);
    	            }
    	        }

    	        // Update the new transitions map
    	        newTransitions.put(newState, newTransStateMap);
    	    }

    	    // Repeat the same for nfa2
    	    for (Map.Entry<String, HashMap<Character, HashSet<String>>> entry : nfa2.getTransitions().entrySet()) {
    	        String originalState = entry.getKey();
    	        HashMap<Character, HashSet<String>> transitions = entry.getValue();

    	        // Get the new state name for the original state from nfa2
    	        String newState = stateUpdater2.get(originalState);
    	        HashMap<Character, HashSet<String>> newTransStateMap = newTransitions.getOrDefault(newState, new HashMap<>());

    	        for (Map.Entry<Character, HashSet<String>> trans : transitions.entrySet()) {
    	            Character symbol = trans.getKey();
    	            HashSet<String> newTargetStates = new HashSet<>();

    	            for (String targetState : trans.getValue()) {
    	                newTargetStates.add(stateUpdater2.get(targetState));
    	            }

    	            if (newTransStateMap.containsKey(symbol)) {
    	                newTransStateMap.get(symbol).addAll(newTargetStates);
    	            } else {
    	                newTransStateMap.put(symbol, newTargetStates);
    	            }
    	        }

    	        // Update the new transitions map
    	        newTransitions.put(newState, newTransStateMap);
    	    }

    	    return newTransitions;
    	}

    
    
    private char[] mergeAlphabets(char[] alph1, char[] alph2) {
        HashSet<Character> alphabetSet = new HashSet<>();
        for (char c : alph1) {
            alphabetSet.add(c);
        }
        for (char c : alph2) {
            alphabetSet.add(c);
        }

        char[] mergedAlphabet = new char[alphabetSet.size()];
        int i = 0;
        for (Character c : alphabetSet) {
            mergedAlphabet[i++] = c;
        }
        return mergedAlphabet;
    }

    
    private void updateStateNames(NFA nfa, HashSet<String> newStates, HashMap<String, String> stateUpdater, HashMap<String, HashMap<Character, HashSet<String>>> newTransitions) {
        int stateCounter = 0;  // This assumes stateCounter is declared at a broader scope if needed elsewhere

        // Iterate over each state in the NFA and create a unique new state name
        for (String state : nfa.getStates()) {
            String newState = state + "_" + stateCounter++;
            stateUpdater.put(state, newState);
            newStates.add(newState);

            // Safely fetch transitions for the state and handle potential null
            HashMap<Character, HashSet<String>> originalTransitions = nfa.getTransitions().get(state);
            if (originalTransitions == null) {
                originalTransitions = new HashMap<>();  // Use an empty map if there are no transitions defined
            }

            // Update transitions to reflect new state names
            HashMap<Character, HashSet<String>> updatedTransitions = new HashMap<>();
            for (Map.Entry<Character, HashSet<String>> entry : originalTransitions.entrySet()) {
                char transitionChar = entry.getKey();
                HashSet<String> targetStates = entry.getValue();
                HashSet<String> updatedTargetStates = new HashSet<>();

                for (String targetState : targetStates) {
                    updatedTargetStates.add(stateUpdater.get(targetState));  // Update target state to its new name
                }

                updatedTransitions.put(transitionChar, updatedTargetStates);  // Map the transition character to the updated set of target states
            }

            newTransitions.put(newState, updatedTransitions);  // Map the new state to its updated transitions
        }
    }

    
    private String[] combineAcceptStates(NFA nfa1, NFA nfa2, HashMap<String, String> stateUpdater1, HashMap<String, String> stateUpdater2) {
        String[] acceptStates1 = nfa1.getAcceptStates();
        String[] acceptStates2 = nfa2.getAcceptStates();
        String[] newAcceptStates = new String[acceptStates1.length + acceptStates2.length];

        int i = 0;
        for (String state : acceptStates1) {
            newAcceptStates[i++] = stateUpdater1.get(state); // Update state names in the list of accept states for NFA1
        }
        for (String state : acceptStates2) {
            newAcceptStates[i++] = stateUpdater2.get(state); // Update state names in the list of accept states for NFA2
        }

        return newAcceptStates;
    }

    
    


    // TODO: Complete this method so that it returns the nfa resulting from concatenating the two input nfas.
    private NFA concatenate(NFA nfa1, NFA nfa2) {
        //combine the states by putting both into a set
        HashSet<String> newStates = new HashSet<>();


        //combine the alphabets by putting both into a set, preventing repeats, then adding back to a new char list
        HashSet<Character> newAlphSet = new HashSet<>();
        for(char letter: nfa1.getAlphabet()) {
            newAlphSet.add(letter);
        }
        for(char letter: nfa2.getAlphabet()) {
            if(!newAlphSet.contains(letter)) {
                newAlphSet.add(letter);
            }
        }
        char[] newAlphList = new char[newAlphSet.size()];
        int i=0;
        for(Character letter: newAlphSet) {
            newAlphList[i] = letter;
            i++;
        }


        //update transitions from original dfas to new transitions
        HashMap<String, HashMap<Character, HashSet<String>>> newTransitions = new HashMap<>(); //state -> (character -> states)
        //updater contains the original state as a key, and a new unique state name as a value
        HashMap<String, String> stateUpdater1 = new HashMap<>();
        HashMap<String, String> stateUpdater2 = new HashMap<>();

        //renames original states to be unique
        for(String state: nfa1.getStates()) {
            stateUpdater1.put(state, state+stateCounter);
            newStates.add(stateUpdater1.get(state));
            stateCounter++;
        }

        //renames internal states to match the way they were renamed above, then puts the whole complete transition into newTransitions
        for(String state: nfa1.getStates()) {
            HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
            for(char letter: newAlphList) {
                HashSet<String> originalStates = nfa1.getTransitions().getOrDefault(state, new HashMap<>()).getOrDefault(letter, new HashSet<>());
                HashSet<String> internalStates = new HashSet<>();
                for(String internalState: originalStates) {
                    internalStates.add(stateUpdater1.get(internalState));
                }
                charToInternalStates.put(letter, internalStates);
            }
            newTransitions.put(stateUpdater1.get(state), charToInternalStates);
        }


        //repeats last 2 chunks with nfa2
        for(String state: nfa2.getStates()) {
            stateUpdater2.put(state, state+stateCounter);
            newStates.add(stateUpdater2.get(state));
            stateCounter++;
        }

        for(String state: nfa2.getStates()) {
            HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
            for(char letter: newAlphList) {
                HashSet<String> originalStates = nfa2.getTransitions().getOrDefault(state, new HashMap<>()).getOrDefault(letter, new HashSet<>());
                HashSet<String> internalStates = new HashSet<>();
                for(String internalState: originalStates) {
                    internalStates.add(stateUpdater2.get(internalState));
                }
                charToInternalStates.put(letter, internalStates);
            }
            newTransitions.put(stateUpdater2.get(state), charToInternalStates);
        }

        //set transitions from accept states in nfa 1 to start state in nfa2

        for(String state: newStates) {
            for (int j = 0; j < nfa1.getAcceptStates().length; j++) {
                if(state.equals(stateUpdater1.get(nfa1.getAcceptStates()[j]))){
                    if(newTransitions.containsKey(state)) {

                        //if an e transition alr exists from an accept state
                        if(newTransitions.get(state).keySet().contains('e')) {
                            HashSet<String> internalStates = new HashSet<>();
                            internalStates = newTransitions.get(state).get('e');
                            internalStates.add(stateUpdater2.get(nfa2.getStartState()));

                            HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
                            charToInternalStates = newTransitions.get(state);
                            charToInternalStates.remove('e');
                            charToInternalStates.put('e', internalStates);
                            newTransitions.put(state, charToInternalStates);


                        }

                        //if a transition exists from an accept state but it isn't an e transition
                        else {
                            HashSet<String> internalStates = new HashSet<>();
                            internalStates.add(stateUpdater2.get(nfa2.getStartState()));
                            HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
                            charToInternalStates = newTransitions.get(state);
                            charToInternalStates.put('e', internalStates);
                            newTransitions.put(state, charToInternalStates);

                        }
                    }

                    else {
                        //accept state doesn't have any transitions coming from it
                        HashSet<String> internalStates = new HashSet<>();
                        HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
                        internalStates.add(stateUpdater2.get(nfa2.getStartState()));
                        charToInternalStates.put('e',internalStates);
                        newTransitions.put(state, charToInternalStates);
                    }
                }
            }
        }


        //set start state
        String newStartState = nfa1.getStartState();

        //combine accept states, just by adding both to a new list
        String[] newAcceptStates = new String[nfa1.getAcceptStates().length+nfa2.getAcceptStates().length];
        i=0;
        for(String state: nfa1.getAcceptStates()) {
            newAcceptStates[i] = stateUpdater1.get(state);
            i++;
        }
        for(String state: nfa2.getAcceptStates()) {
            newAcceptStates[i] = stateUpdater2.get(state);
            i++;
        }


        //make newStates a list
        String[] newStatesList = new String[newStates.size()];
        i = 0;
        for(String state: newStates) {
            newStatesList[i] = state;
            i++;
        }

        //put it all together
        NFA nfaC = new NFA(newStatesList, newAlphList, newTransitions, newStartState, newAcceptStates);
        return nfaC;
    }

    private NFA star(NFA nfa) {
        HashSet<String> newStates = new HashSet<>(Arrays.asList(nfa.getStates()));
        String newStartState = "start" + stateCounter++;
        newStates.add(newStartState);

        char[] newAlphList = nfa.getAlphabet();

        HashMap<String, HashMap<Character, HashSet<String>>> newTransitions = new HashMap<>(nfa.getTransitions());
        HashSet<String> startTransitions = new HashSet<>(Collections.singleton(nfa.getStartState()));

        HashMap<Character, HashSet<String>> startStateTransitions = new HashMap<>();
        startStateTransitions.put('e', startTransitions);
        newTransitions.put(newStartState, startStateTransitions);

        // add epsilon transitions from original accept states back to the original start
        for (String acceptState : nfa.getAcceptStates()) {
            HashMap<Character, HashSet<String>> transitions = newTransitions.getOrDefault(acceptState, new HashMap<>());
            HashSet<String> epsilonTransitions = transitions.getOrDefault('e', new HashSet<>());
            epsilonTransitions.add(nfa.getStartState());
            transitions.put('e', epsilonTransitions);
            newTransitions.put(acceptState, transitions);
        }

        String[] newAcceptStates = Arrays.copyOf(nfa.getAcceptStates(), nfa.getAcceptStates().length + 1);
        newAcceptStates[newAcceptStates.length - 1] = newStartState;

        String[] newStatesArray = newStates.toArray(new String[0]);

        return new NFA(newStatesArray, newAlphList, newTransitions, newStartState, newAcceptStates);
    }

    private NFA plus(NFA nfa) {

        NFA starredNFA = star(nfa);

        // concatenate the original NFA with the starred NFA to make at least one occurrence.
        NFA resultingNFA = concatenate(nfa, starredNFA);

        return new NFA(starredNFA.getStates(), starredNFA.getAlphabet(), starredNFA.getTransitions(),
                starredNFA.getStartState(), nfa.getAcceptStates());
    }

    // TODO: Complete this method so that it returns the nfa that only accepts the character c.
    private NFA singleCharNFA(char c) {
        String[] states = {"S1","S2"};
        char[] alphabet = {c};
        HashMap<String, HashMap<Character, HashSet<String>>> transitions = new HashMap<>();
        HashSet<String> internalStates = new HashSet<>();
        HashMap<Character, HashSet<String>> charToInternalStates = new HashMap<>();
        internalStates.add(states[1]);
        charToInternalStates.put(c, internalStates);
        transitions.put(states[0], charToInternalStates);
        String[] acceptStates = {states[1]};
        NFA nfaSingleChar = new NFA(states, alphabet, transitions, states[0],acceptStates);
        return nfaSingleChar;
    }
    
    

    // You are not allowed to change this method's header at all.
    public boolean test(String string) {
        return nfa.accepts(string);
    }

    // Parser. I strongly recommend you do not change any code below this line.
    // Do not change any of the characters recognized in the regex (e.g., U, *, +, 0, 1)
    private int position = -1, ch;

    public NFA generateNFA() {
        nextChar();
        return parseExpression();
    }

    public void nextChar() {
        ch = (++position < regularExpression.length()) ? regularExpression.charAt(position) : -1;
    }

    public boolean eat(int charToEat) {
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    public NFA parseExpression() {
        NFA nfa = parseTerm();
        while (true) {
            if (eat('U')) {
                // Create the nfa that is the union of the two passed nfas.
                nfa = union(nfa, parseTerm());
            } else {
                return nfa;
            }
        }
    }

    public NFA parseTerm() {
        NFA nfa = parseFactor();
        while (true) {
            // Concatenate NFAs.
            if (ch == '0' || ch == '1' || ch == '(') {
                // Create the nfa that is the concatentaion of the two passed nfas.
                nfa = concatenate(nfa, parseFactor());
            } else {
                return nfa;
            }
        }
    }

    public NFA parseFactor() {
        NFA nfa = null;
        if (eat('(')) {
            nfa = parseExpression();
            if (!eat(')')) {
                throw new RuntimeException("Missing ')'");
            }
        } else if (ch == '0' || ch == '1') {
            // Create the nfa that only accepts the character being passed (regularExpression.charAt(position) == '0' or '1').
            nfa = singleCharNFA(regularExpression.charAt(position));
            nextChar();
        }

        if (eat('*')) {
            // Create the nfa that is the star of the passed nfa.
            nfa = star(nfa);
        } else if (eat('+')) {
            // Create the nfa that is the plus of the passed nfa.
            nfa = plus(nfa);
        }

        return nfa;
    }
}
