package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FagsakMarkering implements EgenskapVerdi, Kodeverdi {

    EØS_BOSATT_NORGE("EØS bosatt Norge", "EØS"),
    BOSATT_UTLAND("Bosatt utland", "Utland"),
    SAMMENSATT_KONTROLL("Sammensatt kontroll", "Kontroll"),
    DØD_DØDFØDSEL("Død eller dødfødsel", "Død"),
    PRAKSIS_UTSETTELSE("Praksis utsettelse", "Utsettelse"),
    BARE_FAR_RETT("Bare far har rett", "BareFar"),
    SELVSTENDIG_NÆRING("Næringsdrivende", "Næring"),
    HASTER("Haster", "Haster");

    @JsonIgnore
    private final String kortNavn;
    @JsonIgnore
    private final String navn;

    FagsakMarkering(String navn, String kortNavn) {
        this.kortNavn = kortNavn;
        this.navn = navn;
    }

    @Override
    public String getVerdi() {
        return name();
    }

    @Override
    public EgenskapNøkkel getNøkkel() {
        return EgenskapNøkkel.FAGSAK_MARKERING;
    }

    private static final Set<FagsakMarkering> PRIORITERT = Set.of(FagsakMarkering.BOSATT_UTLAND, FagsakMarkering.SAMMENSATT_KONTROLL,
        FagsakMarkering.DØD_DØDFØDSEL);

    public static boolean erPrioritert(FagsakMarkering fagsakMarkering) {
        return fagsakMarkering != null && PRIORITERT.contains(fagsakMarkering);
    }

    @Override
    public String getKode() {
        return name();
    }


    @Override
    public String getNavn() {
        return navn;
    }

    public String getKortNavn() {
        return kortNavn;
    }

    private static final Map<String, FagsakMarkering> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.navn, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.name());
            }
        }
    }

}
