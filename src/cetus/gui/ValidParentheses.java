package cetus.gui;

import java.util.LinkedList;

public class ValidParentheses {
	
    public boolean isValid(String s) {
        // Start typing your Java solution below
        // DO NOT write main() function
        LinkedList<Character> stack = new LinkedList<Character>();//must specify the type of the LinkedList
        char[] S = s.toCharArray();
        char prev = ' ';
        for (int i = 0; i < S.length; i++) {
            if (S[i] == '(' || S[i] == '{' || S[i] == '[') {
                stack.push(S[i]);
            } else if (S[i] == ')' || S[i] == '}' || S[i] == ']') {
                if (stack.size() > 0) { //in case no open bracket, only close bracket
                    prev = stack.pop();
                    if (prev == '(' && S[i] != ')' || 
                        prev == '{' && S[i] != '}' || 
                        prev == '[' && S[i] != ']') {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        
        if (stack.size() == 0) {
            return true;
        } else { // if something left,, not valid
            return false;
        }
        
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
