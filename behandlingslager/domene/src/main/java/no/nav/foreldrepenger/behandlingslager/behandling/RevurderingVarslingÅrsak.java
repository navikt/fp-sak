package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RevurderingVarslingÅrsak implements Kodeverdi {

    BARN_IKKE_REGISTRERT_FOLKEREGISTER("BARNIKKEREG", "Barn er ikke registrert i folkeregisteret"),
    ARBEIDS_I_STØNADSPERIODEN("JOBBFULLTID", "Arbeid i stønadsperioden"),
    BEREGNINGSGRUNNLAG_UNDER_HALV_G("IKKEOPPTJENT", "Beregningsgrunnlaget er under 1/2 G"),
    BRUKER_REGISTRERT_UTVANDRET("UTVANDRET", "Bruker er registrert utflyttet"),
    ARBEID_I_UTLANDET("JOBBUTLAND", "Arbeid i utlandet"),
    IKKE_LOVLIG_OPPHOLD("IKKEOPPHOLD", "Ikke lovlig opphold"),
    OPPTJENING_IKKE_OPPFYLT("JOBB6MND", "Opptjeningsvilkår ikke oppfylt"),
    MOR_AKTIVITET_IKKE_OPPFYLT("AKTIVITET", "Mors aktivitetskrav er ikke oppfylt"),
    ANNET("ANNET", "Annet"),
    ;

    private static final Map<String, RevurderingVarslingÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    private final String kode;

    RevurderingVarslingÅrsak(String kode, String navn) {
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
