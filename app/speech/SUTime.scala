package speech

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline._;
import edu.stanford.nlp.time._;
import edu.stanford.nlp.util.CoreMap;

object SUTime {

  def main(args: Array[String]) {
    for (a <- args) {
      println(extract(a, "2017-07-09"))
    }
  }

  /**
   * https://www.garysieling.com/blog/extracting-dates-times-text-stanford-nlp-scala
   */
  def extract(text: String, base: String) = {
    var ret = "";
    
    val props = new Properties();
    val pipeline = new AnnotationPipeline();
    pipeline.addAnnotator(new TokenizerAnnotator(false));
    pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
    pipeline.addAnnotator(new POSTaggerAnnotator(false));
    pipeline.addAnnotator(new TimeAnnotator("sutime", props));
    
    val annotation = new Annotation(text);
    annotation.set(classOf[CoreAnnotations.DocDateAnnotation], base); //"2013-07-14"
    pipeline.annotate(annotation);
    //System.out.println(annotation.get(classOf[CoreAnnotations.TextAnnotation]));
    /*List<CoreMap>*/ val timexAnnsAll = annotation.get(classOf[TimeAnnotations.TimexAnnotations]);
    for (cm <- timexAnnsAll.toArray) {
       /*List<CoreLabel>*/ val tokens = cm.asInstanceOf[Annotation].get(classOf[CoreAnnotations.TokensAnnotation]);
       /*System.out.println(cm + " [from char offset " +
            tokens.get(0).get(classOf[CoreAnnotations.CharacterOffsetBeginAnnotation]) +
            " to " + tokens.get(tokens.size() - 1).get(classOf[CoreAnnotations.CharacterOffsetEndAnnotation]) + ']' +
            " --> " + cm.asInstanceOf[Annotation].get(classOf[TimeExpression.Annotation]).getTemporal()); */
       ret = cm.asInstanceOf[Annotation].get(classOf[TimeExpression.Annotation]).getTemporal().toString
    }
    ret.replaceAll("-WXX-.", "")
  }
}