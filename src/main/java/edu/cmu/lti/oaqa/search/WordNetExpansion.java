package edu.cmu.lti.oaqa.search;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Di Wang.
 */
public class WordNetExpansion {


    static Dictionary dictionary = null;

    static {
        try {
            dictionary = Dictionary.getDefaultResourceInstance();
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }


    public static List<String> getStrictExpansion(String inputWord) {
        List<String> expansion = null;
        try {
            IndexWordSet wordSet = dictionary.lookupAllIndexWords(inputWord);
            if (wordSet.size() == 1) {
                IndexWord indexWord = wordSet.getIndexWordCollection().iterator().next();
                String lemma = indexWord.getLemma();
                List<Synset> senses = indexWord.getSenses();
                if (senses.size() == 1) {
                    List<Word> words = senses.get(0).getWords();
                    if (words.size() > 1) {
                        expansion = new ArrayList<>();
                        for (Word word : words) {
                            if (!word.getLemma().equals(lemma)) {
                                expansion.add(word.getLemma());
                            }
                        }
                    }
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }
        return expansion;
    }

    public static void main(String[] args) {
        List<String> exp = getStrictExpansion("Vampire");
        System.out.println("exp = " + exp);
    }
}
