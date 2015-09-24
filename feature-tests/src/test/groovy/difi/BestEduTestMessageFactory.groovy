package difi
import groovy.text.XmlTemplateEngine
import groovy.xml.XmlUtil

/**
 * Created by mfhoel on 16.09.15.
 */
class BestEduTestMessageFactory {

    public static void main(String[] args) {
        def message = new BestEduTestMessageFactory().createMessage("123", "4321", 100)
        message.writeTo(new PrintWriter(System.out))
    }

    static Writable createMessage(String sender, String reciever, int payloadSize) {

        String data = new byte[payloadSize].encodeBase64()
        String pay = '<?xml version="1.0" encoding="utf-8"?><document><jpId>123</jpId><doc>' << data << '</doc></document>'
        def binding = [sender: sender, reciever: reciever, payload: XmlUtil.escapeXml(pay)]

        def engine = new XmlTemplateEngine()
        def template = engine.createTemplate(new BestEduTestTemplate().text)
        return template.make(binding)
    }
}
