package no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactory;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class OppdragskontrollManagerFactoryKontantytelse implements OppdragskontrollManagerFactory {

    private OppdragskontrollEngangsstønad oppdragskontrollEngangsstønad;
    private RevurderingEndring revurderingEndring;

    OppdragskontrollManagerFactoryKontantytelse() {
        // for CDI proxy
    }

    @Inject
    public OppdragskontrollManagerFactoryKontantytelse(@FagsakYtelseTypeRef("ES") RevurderingEndring revurderingEndring, OppdragskontrollEngangsstønad oppdragskontrollEngangsstønad) {
        this.revurderingEndring = revurderingEndring;
        this.oppdragskontrollEngangsstønad = oppdragskontrollEngangsstønad;
    }

    @Override
    public Optional<OppdragskontrollManager> getManager(Behandling behandling, boolean tidligereOppdragForFagsakFinnes) {
        if (skalSendeOppdrag(behandling)) {
            return Optional.ofNullable(oppdragskontrollEngangsstønad);
        }
        return Optional.empty();
    }

    private boolean skalSendeOppdrag(Behandling behandling) {
        boolean erBeslutningsvedtak = revurderingEndring.erRevurderingMedUendretUtfall(behandling);
        if (behandling.erRevurdering()) {
            return !erBeslutningsvedtak;
        }
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        VedtakResultatType vedtakResultatType = UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingsresultat.getBehandlingResultatType());
        return !erAvslagPåGrunnAvTidligereUtbetaltEngangsstønad(behandlingsresultat, vedtakResultatType)
            && erInnvilgetVedtak(vedtakResultatType, erBeslutningsvedtak);
    }

    private boolean erAvslagPåGrunnAvTidligereUtbetaltEngangsstønad(Behandlingsresultat behandlingsresultat, VedtakResultatType vedtakResultatType) {
        if (VedtakResultatType.AVSLAG.equals(vedtakResultatType)) {
            return Optional.ofNullable(behandlingsresultat.getAvslagsårsak())
                .map(Avslagsårsak::erAlleredeUtbetaltEngangsstønad)
                .orElse(Boolean.FALSE);
        }
        return false;
    }

    private boolean erInnvilgetVedtak(VedtakResultatType vedtakResultatType, boolean erBeslutningsvedtak) {
        return !erBeslutningsvedtak && VedtakResultatType.INNVILGET.equals(vedtakResultatType);
    }
}
