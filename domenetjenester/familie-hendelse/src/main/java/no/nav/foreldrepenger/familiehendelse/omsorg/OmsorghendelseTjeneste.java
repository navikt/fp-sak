package no.nav.foreldrepenger.familiehendelse.omsorg;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.AvklarForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.AvklarOmsorgOgForeldreansvarAksjonspunktData;

@ApplicationScoped
public class OmsorghendelseTjeneste {

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    OmsorghendelseTjeneste() {
        // CDI
    }

    @Inject
    public OmsorghendelseTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    public void aksjonspunktAvklarOmsorgOgForeldreansvar(Behandling behandling, AvklarOmsorgOgForeldreansvarAksjonspunktData data,
                                                         OppdateringResultat.Builder builder) {
        // Omsorgsovertakelse
        OmsorgsovertakelseVilkårType omsorgsovertakelseVilkårType = OmsorgsovertakelseVilkårType.fraKode(data.getVilkarTypeKode());
        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(omsorgsovertakelseVilkårType)
                .medOmsorgsovertakelseDato(data.getOmsorgsovertakelseDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        // Aksjonspunkter
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.getOmsorgsovertakelseAksjonspunkter().contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !Objects.equals(ap.getAksjonspunktDefinisjon(), data.getAksjonspunktDefinisjon())) // ikke avbryte seg selv
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    public void aksjonspunktAvklarForeldreansvar(Behandling behandling, AvklarForeldreansvarAksjonspunktData data) {
        // Omsorgsovertakelse
        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
                .medOmsorgsovertakelseDato(data.getOmsorgsovertakelseDato())
                .medForeldreansvarDato(data.getForeldreansvarDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
    }
}
