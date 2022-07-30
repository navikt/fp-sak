package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.time.LocalDate;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.regler.uttak.konfig.Konfigurasjon;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;

class UtsettelseDokKontrollererFrittUttak implements UtsettelseDokKontrollerer {

    private static final Set<UtsettelseÅrsak> ÅRSAKER_SOM_TRENGER_DOKUMENTASJON_I_TIDSPERIODE_FORBEHOLDT_MOR
        = Set.of(UtsettelseÅrsak.SYKDOM, UtsettelseÅrsak.INSTITUSJON_SØKER, UtsettelseÅrsak.INSTITUSJON_BARN);

    private final LocalDate familiehendelse;

    UtsettelseDokKontrollererFrittUttak(LocalDate familiehendelse) {
        this.familiehendelse = familiehendelse;
    }

    @Override
    public boolean måSaksbehandlerManueltBekrefte(OppgittPeriodeEntitet søknadsperiode) {
        if (!søknadsperiode.isUtsettelse()) {
            throw new IllegalArgumentException("Forventer utsettelse");
        }
        var utsettelseÅrsak = (UtsettelseÅrsak) søknadsperiode.getÅrsak();
        return ÅRSAKER_SOM_TRENGER_DOKUMENTASJON_I_TIDSPERIODE_FORBEHOLDT_MOR.contains(utsettelseÅrsak)
            && søktPeriodeInnenforTidsperiodeForbeholdtMor(søknadsperiode);
    }

    private boolean søktPeriodeInnenforTidsperiodeForbeholdtMor(OppgittPeriodeEntitet søknadsperiode) {
        var tidsperiodeForbeholdtMor = new SimpleLocalDateInterval(fomTidsperiodeForbeholdtMor(familiehendelse),
            TidsperiodeForbeholdtMor.tilOgMed(familiehendelse));
        var søktTidsperiode = new SimpleLocalDateInterval(søknadsperiode.getFom(), søknadsperiode.getTom());
        return søktTidsperiode.overlapper(tidsperiodeForbeholdtMor);
    }

    private static LocalDate fomTidsperiodeForbeholdtMor(LocalDate familiehendelse) {
        return familiehendelse.minusWeeks(
            Konfigurasjon.STANDARD.getParameter(Parametertype.UTTAK_FELLESPERIODE_FØR_FØDSEL_UKER, familiehendelse));
    }
}
