package no.nav.foreldrepenger.historikk;


import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

public class HistorikkInnslagTekstBuilderTest {

    @Test
    public void testHistorikkinnslagTekstSakRetur() {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.SAK_RETUR);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
        Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering = new HashMap<>();

        List<HistorikkinnslagTotrinnsvurdering> vurderingUtenVilkar = new ArrayList<>();

        HistorikkinnslagTotrinnsvurdering vurderPåNytt = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt.setGodkjent(false);
        vurderPåNytt.setBegrunnelse("Må vurderes igjen. Se på dokumentasjon.");

        vurderPåNytt.setAksjonspunktDefinisjon(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
        vurderPåNytt.setAksjonspunktSistEndret(LocalDateTime.now());
        vurdering.put(SkjermlenkeType.SOEKNADSFRIST, Collections.singletonList(vurderPåNytt));

        HistorikkinnslagTotrinnsvurdering godkjent = new HistorikkinnslagTotrinnsvurdering();
        godkjent.setGodkjent(true);
        godkjent.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        godkjent.setAksjonspunktSistEndret(LocalDateTime.now());


        HistorikkinnslagTotrinnsvurdering vurderPåNytt2 = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt2.setGodkjent(false);
        vurderPåNytt2.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        vurderPåNytt2.setBegrunnelse("Ikke enig.");
        vurderPåNytt2.setAksjonspunktSistEndret(LocalDateTime.now());
        vurdering.put(SkjermlenkeType.FAKTA_OM_FOEDSEL, Arrays.asList(godkjent, vurderPåNytt2));

        HistorikkinnslagTotrinnsvurdering vurderPåNytt3 = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt3.setGodkjent(false);
        vurderPåNytt3.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD);
        vurderPåNytt3.setBegrunnelse("Ikke enig.");
        vurderPåNytt3.setAksjonspunktSistEndret(LocalDateTime.now());


        vurderingUtenVilkar.add(vurderPåNytt3);

        List<HistorikkinnslagDel> deler = tekstBuilder
            .medTotrinnsvurdering(vurdering, vurderingUtenVilkar)
            .medHendelse(HistorikkinnslagType.SAK_RETUR)
            .build(historikkinnslag);

        assertThat(deler).hasSize(3);
        HistorikkinnslagDel historikkinnslagDel = deler.get(0);
        List<HistorikkinnslagTotrinnsvurdering> aksjonspunkter = historikkinnslagDel.getTotrinnsvurderinger();
        assertThat(aksjonspunkter).hasSize(1);
        HistorikkinnslagTotrinnsvurdering aksjonspunkt = aksjonspunkter.get(0);
        assertThat(aksjonspunkt.getAksjonspunktDefinisjon()).as("aksjonspunktKode").isEqualTo(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD);
        assertThat(aksjonspunkt.erGodkjent()).as("godkjent").isFalse();
        assertThat(aksjonspunkt.getBegrunnelse()).as("begrunnelse").isEqualTo("Ikke enig.");
    }

}
