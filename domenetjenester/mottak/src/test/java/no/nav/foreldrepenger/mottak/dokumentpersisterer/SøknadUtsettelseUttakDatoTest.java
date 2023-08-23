package no.nav.foreldrepenger.mottak.dokumentpersisterer;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.EndringUtsettelseUttak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadWrapper;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.v3.OmYtelse;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SøknadUtsettelseUttakDatoTest {

    @Test
    void finnUttakUtsettelseFraEndringssøknad() {
        var baseDato = LocalDate.now();
        var uttaksPeriode = new Uttaksperiode();
        uttaksPeriode.setFom(baseDato);
        uttaksPeriode.setTom(baseDato.plusWeeks(2));
        var utsettelsePeriode = new Utsettelsesperiode();
        utsettelsePeriode.setFom(baseDato.plusWeeks(8));
        utsettelsePeriode.setTom(baseDato.plusWeeks(12));
        var fordeling = new Fordeling();
        fordeling.getPerioder().add(utsettelsePeriode);
        fordeling.getPerioder().add(uttaksPeriode);
        var endring = new Endringssoeknad();
        endring.setFordeling(fordeling);
        var omYtelse = new OmYtelse();
        omYtelse.getAny().add(new no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.ObjectFactory().createEndringssoeknad(endring));
        var søknad = new Soeknad();
        søknad.setMottattDato(LocalDate.now());
        søknad.setSoeker(new Bruker());
        søknad.setOmYtelse(omYtelse);

        var wrapper = new SøknadWrapper(søknad);
        var uu = EndringUtsettelseUttak.ekstraherUtsettelseUttakFra(wrapper);
        assertThat(uu.uttakFom()).isEqualTo(baseDato);
        assertThat(uu.utsettelseFom()).isEqualTo(baseDato.plusWeeks(8));
    }

    @Test
    void finnUttakUtsettelseFraFørstegangssøknad() {
        var baseDato = LocalDate.now();
        var utsettelsePeriode = new Utsettelsesperiode();
        var uttaksPeriode = new Uttaksperiode();
        utsettelsePeriode.setFom(baseDato);
        utsettelsePeriode.setTom(baseDato.plusWeeks(2));
        uttaksPeriode.setFom(baseDato.plusWeeks(8));
        uttaksPeriode.setTom(baseDato.plusWeeks(12));
        var fordeling = new Fordeling();
        fordeling.getPerioder().add(utsettelsePeriode);
        fordeling.getPerioder().add(uttaksPeriode);
        var fp = new Foreldrepenger();
        fp.setFordeling(fordeling);
        var omYtelse = new OmYtelse();
        omYtelse.getAny().add(new no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.ObjectFactory().createForeldrepenger(fp));
        var søknad = new Soeknad();
        søknad.setMottattDato(LocalDate.now());
        søknad.setSoeker(new Bruker());
        søknad.setOmYtelse(omYtelse);

        var wrapper = new SøknadWrapper(søknad);
        var uu = EndringUtsettelseUttak.ekstraherUtsettelseUttakFra(wrapper);
        assertThat(uu.utsettelseFom()).isEqualTo(baseDato);
        assertThat(uu.uttakFom()).isEqualTo(baseDato.plusWeeks(8));
    }
}
