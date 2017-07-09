package speech

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline._;
import edu.stanford.nlp.time._;
import edu.stanford.nlp.util.CoreMap;

object SUTime {

  /**
   * https://www.garysieling.com/blog/extracting-dates-times-text-stanford-nlp-scala
   */
  def main(args: Array[String]) {
    val props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

    val pipeline = new StanfordCoreNLP(props);

    val timeAnnotator = new TimeAnnotator()
    pipeline.addAnnotator(timeAnnotator)

    val text =
      "Last summer, they met every Tuesday afternoon, from 1:00 pm to 3:00 pm."
    val doc = new Annotation(text)
    pipeline.annotate(doc)
    
    val timexAnnotations = doc.get(classOf[TimeAnnotations.TimexAnnotations])
    for (timexAnn <- timexAnnotations.toArray) {
      val timeExpr = timexAnn.asInstanceOf[CoreLabel].get(classOf[TimeExpression.Annotation])
      val temporal = timeExpr.getTemporal()
      val range = temporal.getRange()
 
      println(temporal)
      println(range)
    }    
  }
  
  /** Example usage:
   *  java SUTimeDemo "Three interesting dates are 18 Feb 1997, the 20th of july and 4 days from today."
   *
   *  @param args Strings to interpret
   */
  def main2(args:Array[String] ):Unit = {
    val props = new Properties();
    val pipeline = new AnnotationPipeline();
    pipeline.addAnnotator(new TokenizerAnnotator(false));
    pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
    pipeline.addAnnotator(new POSTaggerAnnotator(false));
    pipeline.addAnnotator(new TimeAnnotator("sutime", props));

    for (text <- args) {
      val annotation = new Annotation(text);
      annotation.set(classOf[CoreAnnotations.DocDateAnnotation], "2013-07-14");
      pipeline.annotate(annotation);
      System.out.println(annotation.get(classOf[CoreAnnotations.TextAnnotation]));
      /*List<CoreMap>*/ val timexAnnsAll = annotation.get(classOf[TimeAnnotations.TimexAnnotations]);
      for (cm <- timexAnnsAll.toArray) {
        /*List<CoreLabel>*/ val tokens = cm.asInstanceOf[Annotation].get(classOf[CoreAnnotations.TokensAnnotation]);
        System.out.println(cm + " [from char offset " +
            tokens.get(0).get(classOf[CoreAnnotations.CharacterOffsetBeginAnnotation]) +
            " to " + tokens.get(tokens.size() - 1).get(classOf[CoreAnnotations.CharacterOffsetEndAnnotation]) + ']' +
            " --> " + cm.asInstanceOf[CoreLabel].get(classOf[TimeExpression.Annotation]).getTemporal());
      }
      System.out.println("--");
    }
  }
}