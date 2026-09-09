package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TilbakekrevingVidereBehandling implements Kodeverdi, DatabaseKode {

    UDEFINIERT(STANDARDKODE_UDEFINERT, "Udefinert."),
    OPPRETT_TILBAKEKREVING("TILBAKEKR_OPPRETT", "Opprett tilbakekreving"),
    IGNORER_TILBAKEKREVING("TILBAKEKR_IGNORER", "Feilutbetaling, avvent samordning"),
    INNTREKK("TILBAKEKR_INNTREKK", "Feilutbetalingen er trukket inn i annen utbetaling"),
    TILBAKEKR_OPPDATER("TILBAKEKR_OPPDATER", "Endringer vil oppdatere eksisterende feilutbetalte perioder og beløp."),
    ;

    /**
     * OPPRETT_TILBAKEKREVING dekker to valg saksbehandler kan gjøre - med og uten varsel til bruker.
     * Skillet ligger i om varseltekst er satt, ikke i selve kodeverdien.
     */
    public static String navnForHistorikk(TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        if (videreBehandling == null) {
            return null;
        }
        if (OPPRETT_TILBAKEKREVING.equals(videreBehandling)) {
            var varselsuffiks = varseltekst == null || varseltekst.isBlank() ? ", ikke send varsel" : ", send varsel";
            return videreBehandling.getNavn() + varselsuffiks;
        }
        return videreBehandling.getNavn();
    }

    private static final Map<String, TilbakekrevingVidereBehandling> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    TilbakekrevingVidereBehandling(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

}
