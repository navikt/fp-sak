package no.nav.foreldrepenger.familiehendelse.omsorg;

import java.util.Objects;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.domene.personopplysning.AvklarForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.domene.personopplysning.AvklarOmsorgOgForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.domene.personopplysning.AvklartDataBarnAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

class AvklarOmsorgOgForeldreansvarAksjonspunkt {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    AvklarOmsorgOgForeldreansvarAksjonspunkt(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    void oppdater(Behandling behandling, AvklarForeldreansvarAksjonspunktData data) {
        // Omsorgsovertakelse
        avklareOmsorgovertakelse(behandling, data);
    }

    void oppdater(Behandling behandling, AvklarOmsorgOgForeldreansvarAksjonspunktData data, OppdateringResultat.Builder builder) {
        // Omsorgsovertakelse
        avklareOmsorgovertakelse(behandling, data);

        // Aksjonspunkter
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.getOmsorgsovertakelseAksjonspunkter().contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !Objects.equals(ap.getAksjonspunktDefinisjon(), data.getAksjonspunktDefinisjon())) // ikke avbryte seg selv
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    private void avklareOmsorgovertakelse(Behandling behandling, AvklarForeldreansvarAksjonspunktData data) {
        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
                .medOmsorgsovertakelseDato(data.getOmsorgsovertakelseDato())
                .medForeldreansvarDato(data.getForeldreansvarDato()))
            .tilbakestillBarn()
            .medAntallBarn(data.getAntallBarn());
        for (AvklartDataBarnAdapter avklartDataBarnAdapter : data.getBarn()) {
            oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(avklartDataBarnAdapter.getFødselsdato(), avklartDataBarnAdapter.getNummer()));
        }
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
    }

    private void avklareOmsorgovertakelse(Behandling behandling, AvklarOmsorgOgForeldreansvarAksjonspunktData data) {
        OmsorgsovertakelseVilkårType omsorgsovertakelseVilkårType = OmsorgsovertakelseVilkårType.fraKode(data.getVilkarTypeKode());
        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(omsorgsovertakelseVilkårType)
                .medOmsorgsovertakelseDato(data.getOmsorgsovertakelseDato()))
            .tilbakestillBarn()
            .medAntallBarn(data.getAntallBarn());
        for (AvklartDataBarnAdapter avklartDataBarnAdapter : data.getBarn()) {
            oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(avklartDataBarnAdapter.getFødselsdato(), avklartDataBarnAdapter.getNummer()));
        }
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
    }

}
