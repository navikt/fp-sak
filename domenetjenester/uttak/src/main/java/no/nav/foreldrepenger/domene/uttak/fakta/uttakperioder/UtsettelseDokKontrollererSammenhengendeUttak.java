package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FERIE;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

class UtsettelseDokKontrollererSammenhengendeUttak implements UtsettelseDokKontrollerer {
    @Override
    public boolean måSaksbehandlerManueltBekrefte(OppgittPeriodeEntitet søknadsperiode) {
        if (!søknadsperiode.isUtsettelse()) {
            throw new IllegalArgumentException("Forventer utsettelse");
        }
        var utsettelseÅrsak = søknadsperiode.getÅrsak();
        return !ARBEID.equals(utsettelseÅrsak) && !FERIE.equals(utsettelseÅrsak);
    }
}
