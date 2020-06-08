package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class ErEndringIUttakImpl implements ErEndringIUttak {

    @Override
    public boolean vurder(UttakResultatHolder uttakresultatRevurdering, UttakResultatHolder uttakresultatOriginal) {
        return uttakresultatOriginal.vurderOmErEndringIUttak(uttakresultatRevurdering);
    }

}

