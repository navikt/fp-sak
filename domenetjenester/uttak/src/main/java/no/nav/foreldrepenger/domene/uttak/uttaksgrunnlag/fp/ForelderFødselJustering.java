package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

public interface ForelderFødselJustering {
    List<OppgittPeriodeEntitet> justerVedFødselEtterTermin(List<OppgittPeriodeEntitet> oppgittePerioder);

    List<OppgittPeriodeEntitet> justerVedFødselFørTermin(List<OppgittPeriodeEntitet> oppgittePerioder);
}
