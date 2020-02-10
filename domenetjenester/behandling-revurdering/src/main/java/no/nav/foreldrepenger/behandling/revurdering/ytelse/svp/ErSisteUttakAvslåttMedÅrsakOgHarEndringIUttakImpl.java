package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.SettOpphørOgIkkeRett;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
class ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttakImpl implements ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak {

    @Inject
    public ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttakImpl() {
    }

    @Override
    public boolean vurder(UttakResultatHolder uttakresultat, boolean erEndringIUttakFraEndringstidspunkt) {
        return erEndringIUttakFraEndringstidspunkt && uttakresultat.kontrollerErSisteUttakAvslåttMedÅrsak();
    }


    @Override
    public Behandlingsresultat fastsett(Behandling revurdering) {
        return SettOpphørOgIkkeRett.fastsett(revurdering, Vedtaksbrev.AUTOMATISK);
    }

}
