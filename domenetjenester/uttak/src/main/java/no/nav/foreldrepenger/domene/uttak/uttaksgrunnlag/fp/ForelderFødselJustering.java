package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

import java.util.List;

public interface ForelderFødselJustering {
    List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder);

    List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder);
}
