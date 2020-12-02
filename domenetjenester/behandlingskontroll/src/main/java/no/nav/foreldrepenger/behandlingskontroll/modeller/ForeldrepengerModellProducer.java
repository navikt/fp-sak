package no.nav.foreldrepenger.behandlingskontroll.modeller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class ForeldrepengerModellProducer {

    private static final String YTELSE = "FP";
    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.FORELDREPENGER;

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-002")
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        modellBuilder.medSteg(
                BehandlingStegType.REGISTRER_SØKNAD,
                BehandlingStegType.INNHENT_SØKNADOPP,
                BehandlingStegType.VURDER_KOMPLETTHET,
                BehandlingStegType.INNHENT_REGISTEROPP,
                BehandlingStegType.INREG_AVSL,
                BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD,
                BehandlingStegType.KONTROLLER_FAKTA,
                BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
                BehandlingStegType.SØKERS_RELASJON_TIL_BARN,
                BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
                BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE,
                BehandlingStegType.VURDER_OPPTJENING_FAKTA,
                BehandlingStegType.VURDER_OPPTJENINGSVILKÅR,
                BehandlingStegType.VURDER_SAMLET,
                BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
                BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
                BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
                BehandlingStegType.VURDER_REF_BERGRUNN,
                BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
                BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG,
                BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP,
                BehandlingStegType.SØKNADSFRIST_FORELDREPENGER,
                BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
                BehandlingStegType.VURDER_UTTAK,
                BehandlingStegType.BEREGN_YTELSE,
                BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT,
                BehandlingStegType.SIMULER_OPPDRAG,
                BehandlingStegType.VURDER_FARESIGNALER,
                BehandlingStegType.FORESLÅ_VEDTAK,
                BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-004")
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, YTELSE_TYPE);
        modellBuilder.medSteg(
                BehandlingStegType.VARSEL_REVURDERING,
                BehandlingStegType.REGISTRER_SØKNAD,
                BehandlingStegType.INNHENT_SØKNADOPP,
                BehandlingStegType.VURDER_KOMPLETTHET,
                BehandlingStegType.INNHENT_REGISTEROPP,
                BehandlingStegType.INREG_AVSL,
                BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD,
                BehandlingStegType.KONTROLLER_FAKTA,
                BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
                BehandlingStegType.SØKERS_RELASJON_TIL_BARN,
                BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
                BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE,
                BehandlingStegType.VURDER_OPPTJENING_FAKTA,
                BehandlingStegType.VURDER_OPPTJENINGSVILKÅR,
                BehandlingStegType.VURDER_SAMLET,
                BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
                BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
                BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
                BehandlingStegType.VURDER_REF_BERGRUNN,
                BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
                BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG,
                BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP,
                BehandlingStegType.SØKNADSFRIST_FORELDREPENGER,
                BehandlingStegType.VULOMED,
                BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
                BehandlingStegType.VURDER_UTTAK,
                BehandlingStegType.BEREGN_YTELSE,
                BehandlingStegType.VURDER_TILBAKETREKK,
                BehandlingStegType.HINDRE_TILBAKETREKK,
                BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT,
                BehandlingStegType.SIMULER_OPPDRAG,
                BehandlingStegType.FORESLÅ_VEDTAK,
                BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-006")
    @Produces
    @ApplicationScoped
    public BehandlingModell innsyn() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.INNSYN, YTELSE_TYPE);
        modellBuilder.medSteg(
                BehandlingStegType.INNHENT_PERSONOPPLYSNINGER,
                BehandlingStegType.VURDER_INNSYN,
                BehandlingStegType.FORESLÅ_VEDTAK,
                BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-003")
    @Produces
    @ApplicationScoped
    public BehandlingModell klage() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.KLAGE, YTELSE_TYPE);
        modellBuilder.medSteg(
                BehandlingStegType.KLAGE_VURDER_FORMKRAV_NFP,
                BehandlingStegType.KLAGE_NFP,
                BehandlingStegType.KLAGE_VURDER_FORMKRAV_NK,
                BehandlingStegType.KLAGE_NK,
                BehandlingStegType.FORESLÅ_VEDTAK,
                BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-008")
    @Produces
    @ApplicationScoped
    public BehandlingModell anke() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.ANKE, YTELSE_TYPE);
        modellBuilder.medSteg(
                BehandlingStegType.ANKE,
                BehandlingStegType.FORESLÅ_VEDTAK,
                BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.ANKE_MERKNADER,
                BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
