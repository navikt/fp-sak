package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.Period;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristRegelOrkestrering;
import no.nav.foreldrepenger.regler.soknadsfrist.SøknadsfristResultat;
import no.nav.foreldrepenger.regler.soknadsfrist.grunnlag.SøknadsfristGrunnlag;

class SøknadsfristRegelAdapter {

    SøknadsfristResultat vurderSøknadsfristFor(SøknadEntitet søknad, List<OppgittPeriodeEntitet> oppgittePerioder, Period periodeSøknadsfrist) {
        SøknadsfristGrunnlag søknadsfristGrunnlag = SøknadsfristRegelOversetter.tilGrunnlag(søknad, oppgittePerioder, periodeSøknadsfrist);
        SøknadsfristRegelOrkestrering regelOrkestrering = new SøknadsfristRegelOrkestrering();
        return regelOrkestrering.vurderSøknadsfrist(søknadsfristGrunnlag);
    }
}
