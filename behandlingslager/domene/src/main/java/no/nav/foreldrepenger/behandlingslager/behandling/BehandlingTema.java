package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum BehandlingTema implements Kodeverdi, MedOffisiellKode {

    ENGANGSSTØNAD("ENGST", "Engangsstønad", "ab0327"),
    ENGANGSSTØNAD_FØDSEL("ENGST_FODS", "Engangsstønad ved fødsel", "ab0050"),
    ENGANGSSTØNAD_ADOPSJON("ENGST_ADOP", "Engangsstønad ved adopsjon", "ab0027"),
    FORELDREPENGER("FORP", "Foreldrepenger", "ab0326"),
    FORELDREPENGER_ADOPSJON("FORP_ADOP", "Foreldrepenger ved adopsjon", "ab0072"),
    FORELDREPENGER_FØDSEL("FORP_FODS", "Foreldrepenger ved fødsel", "ab0047"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger", "ab0126"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),

    ;

    private final String navn;

    private final String offisiellKode;
    @JsonValue
    private final String kode;

    BehandlingTema(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static BehandlingTema finnForKodeverkEiersKode(String offisiellDokumentType) {
        return Stream.of(values()).filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    public static boolean gjelderEngangsstønad(BehandlingTema behandlingTema) {
        return ENGANGSSTØNAD_ADOPSJON.equals(behandlingTema) || ENGANGSSTØNAD_FØDSEL.equals(behandlingTema) || ENGANGSSTØNAD.equals(behandlingTema);
    }

    public static boolean gjelderForeldrepenger(BehandlingTema behandlingTema) {
        return FORELDREPENGER_ADOPSJON.equals(behandlingTema) || FORELDREPENGER_FØDSEL.equals(behandlingTema) || FORELDREPENGER.equals(behandlingTema);
    }

    public static boolean gjelderSvangerskapspenger(BehandlingTema behandlingTema) {
        return SVANGERSKAPSPENGER.equals(behandlingTema);
    }

    public static boolean ikkeSpesifikkHendelse(BehandlingTema behandlingTema) {
        return FORELDREPENGER.equals(behandlingTema) || ENGANGSSTØNAD.equals(behandlingTema) || UDEFINERT.equals(behandlingTema);
    }

    public static boolean gjelderSammeYtelse(BehandlingTema tema1, BehandlingTema tema2) {
        return gjelderForeldrepenger(tema1) && gjelderForeldrepenger(tema2) || gjelderEngangsstønad(tema1) && gjelderEngangsstønad(tema2)
            || gjelderSvangerskapspenger(tema1) && gjelderSvangerskapspenger(tema2);

    }

    /**
     * Returnerer true hvis angitt tema gjelder samme ytelse og hendelse som denne. Hendlse trenger kun matche hvis begge temaene amgir dette
     * eksplisitt.
     */
    public boolean erKompatibelMed(BehandlingTema that) {
        return gjelderSammeYtelse(this, that) && (ikkeSpesifikkHendelse(this) || ikkeSpesifikkHendelse(that) || equals(that));
    }

    public static BehandlingTema fraFagsak(Fagsak fagsak, FamilieHendelseEntitet hendelse) {
        return fraFagsak(fagsak.getYtelseType(), hendelse);
    }

    public static BehandlingTema fraFagsak(FagsakYtelseType ytelseType, FamilieHendelseEntitet hendelse) {
        var hendelseType = hendelse != null ? hendelse.getType() : FamilieHendelseType.UDEFINERT;
        return fraFagsakHendelse(ytelseType, hendelseType);
    }

    public static BehandlingTema fraFagsakHendelse(FagsakYtelseType ytelseType, FamilieHendelseType hendelseType) {

        if (FamilieHendelseType.gjelderFødsel(hendelseType)) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
                return ENGANGSSTØNAD_FØDSEL;
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
                return FORELDREPENGER_FØDSEL;
            }
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
                return SVANGERSKAPSPENGER;
            }
        } else if (FamilieHendelseType.gjelderAdopsjon(hendelseType)) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
                return ENGANGSSTØNAD_ADOPSJON;
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
                return FORELDREPENGER_ADOPSJON;
            }
        } else if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return ENGANGSSTØNAD;
        } else if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return FORELDREPENGER;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return SVANGERSKAPSPENGER;
        }

        return UDEFINERT;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        if (BehandlingTema.gjelderForeldrepenger(this)) {
            return FagsakYtelseType.FORELDREPENGER;
        }
        if (BehandlingTema.gjelderEngangsstønad(this)) {
            return FagsakYtelseType.ENGANGSTØNAD;
        }
        if (BehandlingTema.gjelderSvangerskapspenger(this)) {
            return FagsakYtelseType.SVANGERSKAPSPENGER;
        }
        return FagsakYtelseType.UDEFINERT;
    }
}
