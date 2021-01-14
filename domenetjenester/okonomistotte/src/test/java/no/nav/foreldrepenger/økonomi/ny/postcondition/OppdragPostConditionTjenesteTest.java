package no.nav.foreldrepenger.økonomi.ny.postcondition;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Sats;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;

public class OppdragPostConditionTjenesteTest {

    LocalDate forrigeSøndag = LocalDate.of(2020, 11, 22);
    LocalDate mandag = forrigeSøndag.plusDays(1);
    LocalDate fredag = mandag.plusDays(4);
    LocalDate lørdag = mandag.plusDays(5);
    LocalDate søndag = mandag.plusDays(6);
    LocalDate nesteMandag = mandag.plusDays(7);

    @Test
    public void skal_ikke_finne_differanse_mellom_identiske_ytelser() {
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(Ytelse.EMPTY, Ytelse.EMPTY, Betalingsmottaker.BRUKER)).isEmpty();
    }

    @Test
    public void skal_ignorere_helger_for_satstype_dagsats() {
        Ytelse ytelseKontinuerlig = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(mandag, fredag), Sats.dagsats(100)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(nesteMandag, nesteMandag), Sats.dagsats(100)))
            .build();
        Ytelse ytelseSplittet = Ytelse.builder().leggTilPeriode(
            new YtelsePeriode(Periode.of(mandag, nesteMandag), Sats.dagsats(100)))
            .build();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseKontinuerlig, ytelseSplittet, Betalingsmottaker.BRUKER)).isEmpty();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseSplittet, ytelseKontinuerlig, Betalingsmottaker.BRUKER)).isEmpty();
    }

    @Test
    public void skal_finne_differanse() {
        Ytelse ytelse1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, mandag), Sats.dagsats(100))).build();

        assertThat(OppdragPostConditionTjeneste.finnDifferanse(Ytelse.EMPTY, ytelse1, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, 100));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse1, Ytelse.EMPTY, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, -100));

        Ytelse ytelse1SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Sats.dagsats(100))).build();
        Ytelse ytelse2SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Sats.dagsats(200))).build();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse1SøndagSøndag, ytelse2SøndagSøndag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, 500));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse2SøndagSøndag, ytelse1SøndagSøndag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, -500));

        Ytelse ytelseUke1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, lørdag), Sats.dagsats(100))).build();
        Ytelse ytelseUke1OgNesteMandag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, nesteMandag), Sats.dagsats(100))).build();

        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseUke1, ytelseUke1OgNesteMandag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(nesteMandag, null, 100));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseUke1OgNesteMandag, ytelseUke1, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(nesteMandag, null, -100));
    }
}
