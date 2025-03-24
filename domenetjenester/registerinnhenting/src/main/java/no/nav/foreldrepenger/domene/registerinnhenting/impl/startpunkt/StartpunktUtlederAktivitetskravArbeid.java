package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

import java.util.Optional;

@ApplicationScoped
@GrunnlagRef(AktivitetskravGrunnlagEntitet.GRUNNLAG_NAME)
class StartpunktUtlederAktivitetskravArbeid implements StartpunktUtleder {

    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;
    private BehandlingRepository behandlingRepository;

    StartpunktUtlederAktivitetskravArbeid() {
        // For CDI
    }

    @Inject
    StartpunktUtlederAktivitetskravArbeid(AktivitetskravArbeidRepository aktivitetskravArbeidRepository, BehandlingRepository behandlingRepository) {
        this.aktivitetskravArbeidRepository = aktivitetskravArbeidRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = aktivitetskravArbeidRepository.hentGrunnlagPåId((Long) grunnlagId1);
        var grunnlag2 = aktivitetskravArbeidRepository.hentGrunnlagPåId((Long) grunnlagId2);

        var differ = new RegisterdataDiffsjekker(true);
        if (differ.erForskjellPå(grunnlag1, grunnlag2)) {
            var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
            if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON)) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "aktivitetskravArbeid",
                        Optional.ofNullable(grunnlagId1).orElse(""), Optional.ofNullable(grunnlagId2).orElse(""));
                return StartpunktType.UTTAKSVILKÅR;
            } else {
                return StartpunktType.UDEFINERT;
            }
        } else{
            return StartpunktType.UDEFINERT;
        }
    }

}
