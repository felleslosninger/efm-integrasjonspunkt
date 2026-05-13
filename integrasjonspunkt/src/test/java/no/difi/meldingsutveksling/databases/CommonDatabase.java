package no.difi.meldingsutveksling.databases;

public class CommonDatabase {

    public static final String[] TABLES = {
        "business_message_file", "conversation", "forsendelse_id_entry", "message_status", "message_channel_entry",
        "next_move_message_entry", "next_move_message", "sas_key_wrapper", "webhook_subscription" };

    private CommonDatabase() { /* intentionally non-instantiable */ }

}
