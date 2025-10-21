package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.app.HentKodeverkTjeneste;

class KodeverkRestTjenesteTest {

    private HentKodeverkTjeneste hentKodeverkTjeneste;

    @BeforeEach
    void before() {
        hentKodeverkTjeneste = new HentKodeverkTjeneste();
    }

    @Test
    void skal_hente_kodeverk_og_gruppere_på_kodeverknavn() throws IOException {

        var tjeneste = new KodeverkRestTjeneste(hentKodeverkTjeneste);
        var response = tjeneste.hentGruppertKodeliste();

        var rawJson = (String) response.getEntity();
        assertThat(rawJson).isNotNull();

        Map<String, Object> gruppertKodeliste = new JacksonJsonConfig().getObjectMapper().readValue(rawJson, Map.class);

        assertThat(gruppertKodeliste)
                .containsKeys(FagsakStatus.class.getSimpleName(), Avslagsårsak.class.getSimpleName(), Landkoder.class.getSimpleName());

        assertThat(gruppertKodeliste.keySet())
                .containsAll(new HashSet<>(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.keySet()));

        assertThat(gruppertKodeliste.keySet()).hasSize(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.size());

        var fagsakStatuser = (List<Map<String, String>>) gruppertKodeliste.get(FagsakStatus.class.getSimpleName());
        assertThat(fagsakStatuser.stream().map(k -> k.get("kode")).toList()).contains(FagsakStatus.AVSLUTTET.getKode(),
                FagsakStatus.OPPRETTET.getKode());

        var map = (Map<String, List<?>>) gruppertKodeliste.get(Avslagsårsak.class.getSimpleName());
        assertThat(map).containsKeys(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.getKode(), VilkårType.MEDLEMSKAPSVILKÅRET.getKode());

        var avslagsårsaker = (List<Map<String, String>>) map.get(VilkårType.OMSORGSOVERTAKELSEVILKÅR.getKode());
        assertThat(avslagsårsaker.stream().map(k -> ((Map) k).get("kode")).toList())
                .contains(Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR.getKode(),
                        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR.getKode());
    }

    @Test
    void serialize_kodeverdi_enums() throws Exception {
        var jsonConfig = new JacksonJsonConfig();

        var om = jsonConfig.getObjectMapper();

        var json = om.writer().withDefaultPrettyPrinter().writeValueAsString(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);

        assertThat(json).isEqualTo("\"7001\"");
    }

    @Test
    void serialize_kodeverdi_uttak() throws Exception {

        var jsonConfig = new JacksonJsonConfig();

        var om = jsonConfig.getObjectMapper();

        var json = om.writer().withDefaultPrettyPrinter().writeValueAsString(new X(PeriodeResultatÅrsak.STØNADSPERIODE_NYTT_BARN));

        assertThat(json).contains("\"periodeResultatÅrsak\" : \"4104\"");
    }

    @Test
    void serialize_kodeverdi_uttak_full() throws Exception {

        var jsonConfig = new JacksonJsonConfig(true);

        var om = jsonConfig.getObjectMapper();

        var json = om.writer().withDefaultPrettyPrinter().writeValueAsString(new X(PeriodeResultatÅrsak.STØNADSPERIODE_NYTT_BARN));

        assertThat(json).contains("\"kode\" : \"4104\"")
            .contains("\"utfallType\" : \"AVSLÅTT\"");
    }

    private record X(PeriodeResultatÅrsak periodeResultatÅrsak) {}

}
