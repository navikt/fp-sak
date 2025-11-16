package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.app.HentKodeverdierTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.KodeverdiMedNavnDto;

class KodeverkRestTjenesteTest {

    @Test
    void skal_hente_kodeverk_og_gruppere_på_kodeverknavn_v2() {

        var tjeneste = new KodeverkRestTjeneste();
        var response = tjeneste.hentGruppertKodelisteMedNavn();

        Map<String, Collection<KodeverdiMedNavnDto>> gruppertKodeliste = (Map<String, Collection<KodeverdiMedNavnDto>>)response.getEntity();

        assertThat(gruppertKodeliste)
            .containsKeys(FagsakStatus.class.getSimpleName(), Avslagsårsak.class.getSimpleName(), Landkoder.class.getSimpleName());

        assertThat(gruppertKodeliste.keySet())
            .containsAll(new HashSet<>(HentKodeverdierTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.keySet()));

        assertThat(gruppertKodeliste.keySet()).hasSize(HentKodeverdierTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.size());

        var fagsakStatuser = gruppertKodeliste.get(FagsakStatus.class.getSimpleName());
        assertThat(fagsakStatuser.stream().map(k -> k.kode()).toList()).contains(FagsakStatus.AVSLUTTET.getKode(),
            FagsakStatus.OPPRETTET.getKode());

        var avslagsårsaker = gruppertKodeliste.get(Avslagsårsak.class.getSimpleName());
        assertThat(avslagsårsaker.stream().map(k -> k.kode()).toList())
            .contains(Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT.getKode(), Avslagsårsak.BARN_OVER_15_ÅR.getKode());
    }

}
