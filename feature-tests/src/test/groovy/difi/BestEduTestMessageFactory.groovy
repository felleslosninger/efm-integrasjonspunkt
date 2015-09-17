package difi

import groovy.text.XmlTemplateEngine
/**
 * Created by mfhoel on 16.09.15.
 */
class BestEduTestMessageFactory {

    public static void main(String[] args) {
        def message = new BestEduTestMessageFactory().createMessage(100)
        message.writeTo(new PrintWriter(System.out))
    }

    Writable createMessage(String sender, String reciever, int payloadSize) {

        byte[] payload = new byte[payloadSize]
        def binding = [sender: sender, reciever: reciever, payload: payload]

        def engine = new XmlTemplateEngine()
        def template = engine.createTemplate(new BestEduTestTemplate().text)
        def writable = template.make(binding)
    }
}
