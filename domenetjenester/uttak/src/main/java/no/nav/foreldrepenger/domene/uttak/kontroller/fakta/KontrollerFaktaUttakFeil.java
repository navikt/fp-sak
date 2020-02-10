package no.nav.foreldrepenger.domene.uttak.kontroller.fakta;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import java.time.LocalDate;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface KontrollerFaktaUttakFeil extends DeklarerteFeil {
    KontrollerFaktaUttakFeil FACTORY = FeilFactory.create(KontrollerFaktaUttakFeil.class);

    @TekniskFeil(feilkode = "FP-823386", feilmelding = "Datafeil. Periode er dokumentert uten at saksbehandler har begrunnet dette.", logLevel = WARN)
    Feil dokumentertUtenBegrunnelse();

    @TekniskFeil(feilkode = "FP-651234", feilmelding = "Ikke gyldig søknadsperiode. Periode med gradering for arbeidstaker trenger arbeidsgiver oppgitt. %s %s %s", logLevel = ERROR)
    Feil søktGraderingUtenArbeidsgiver(String periodeType, LocalDate fom, LocalDate tom);

}
