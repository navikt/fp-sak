package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class JusterFordelingTjenesteTest {

    private LocalDate fødselsdato = LocalDate.of(2018, 1, 1);
    private JusterFordelingTjeneste justerFordelingTjeneste = new JusterFordelingTjeneste();

    @Test
    public void normal_case_føder_på_termin() {
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(15).minusDays(1));
        OppgittPeriodeEntitet fp = lagPeriode(UttakPeriodeType.FELLESPERIODE, fødselsdato.plusWeeks(15), fødselsdato.plusWeeks(31).minusDays(1));
        List<OppgittPeriodeEntitet> oppgittePerioder = List.of(fpff, mk, fp);

        //Føder på termin
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, fødselsdato);

        //Oppgitteperioder er uendret
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isTrue();
    }

    @Test
    public void foreldrepenger_før_fødsel_forkortes_ved_for_tidlig_fødsel() {
        LocalDate fødselsdato = LocalDate.of(2019, 1, 1);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var oppgittePerioder = List.of(fpff);

        //Føder en dag før termin
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, fødselsdato.minusDays(1));

        //Periode skal flyttes 1 dag tidligere, men ikke før første uttak
        assertThat(justertePerioder).hasSize(1);
        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        //flyttes fra mandag til fredag
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1).minusDays(3));
    }

    @Test
    public void foreldrepenger_før_fødsel_forkortes_ved_for_tidlig_fødsel_flere_perioder() {
        LocalDate fødselsdato = LocalDate.of(2019, 1, 14);
        OppgittPeriodeEntitet fpff1 = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusWeeks(1));
        //Denne blir helt borte
        OppgittPeriodeEntitet fpff2 = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(1).plusDays(1), fødselsdato.minusDays(1));
        var oppgittePerioder = List.of(fpff1, fpff2);

        //En dag igjen til fpff
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, fødselsdato.minusWeeks(3).plusDays(1));

        assertThat(justertePerioder).hasSize(1);
        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getFom()).isEqualTo(fpff1.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusWeeks(3));
    }

    @Test
    public void antall_virkedager_for_periode_skal_være_det_samme_etter_justering() {
        //mandag
        LocalDate termin = LocalDate.of(2018, 10, 1);

        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(4));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(termin.plusWeeks(1), termin.plusWeeks(2).minusDays(3));
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(2), termin.plusWeeks(3));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode, utsettelse);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusDays(4));

        assertThat(justertePerioder).hasSize(5);

        OppgittPeriodeEntitet justertMødrekvoteFørSplitt = justertePerioder.get(1);
        assertThat(justertMødrekvoteFørSplitt.getFom()).isEqualTo(LocalDate.of(2018, 10, 5));
        assertThat(justertMødrekvoteFørSplitt.getTom()).isEqualTo(LocalDate.of(2018, 10, 5));

        OppgittPeriodeEntitet justertUtsettelse = justertePerioder.get(2);
        assertThat(justertUtsettelse.getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertUtsettelse.getTom()).isEqualTo(utsettelse.getTom());

        OppgittPeriodeEntitet justertMødrekvoteEtterSplitt = justertePerioder.get(3);
        assertThat(justertMødrekvoteEtterSplitt.getFom()).isEqualTo(LocalDate.of(2018, 10, 15));
        assertThat(justertMødrekvoteEtterSplitt.getTom()).isEqualTo(LocalDate.of(2018, 10, 18));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(4);
        assertThat(justertFellesperiode.getFom()).isEqualTo(LocalDate.of(2018, 10, 19));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void siste_periode_blir_fjernet_helt() {
        //mandag
        LocalDate termin = LocalDate.of(2018, 10, 1);

        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(4));

        var oppgittePerioder = List.of(mødrekvote);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusDays(10));

        assertThat(justertePerioder).hasSize(1);

        OppgittPeriodeEntitet fellesperiode = justertePerioder.get(0);
        assertThat(fellesperiode.getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(fellesperiode.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    public void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_siste_periode_er_ikke_flyttbar() {
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 23));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 26), LocalDate.of(2019, 8, 26));

        var oppgittePerioder = List.of(mødrekvote, utsettelse);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(2);

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(0);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(5));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    public void skal_slå_sammen_like_perioder() {
        OppgittPeriodeEntitet mødrekvote1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        OppgittPeriodeEntitet mødrekvote2 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        OppgittPeriodeEntitet mødrekvote3 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 22), LocalDate.of(2019, 8, 22));
        OppgittPeriodeEntitet mødrekvote4 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 23), LocalDate.of(2019, 8, 23));

        var oppgittePerioder = List.of(mødrekvote1, mødrekvote2, mødrekvote3, utsettelse, mødrekvote4);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom());

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet justertMødrekvote1 = justertePerioder.get(0);
        assertThat(justertMødrekvote1.getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(justertMødrekvote1.getTom()).isEqualTo(mødrekvote3.getTom());
    }

    @Test
    public void skal_slå_sammen_like_perioder_usortert() {
        OppgittPeriodeEntitet foreldrepenger1 = lagPeriode(UttakPeriodeType.FORELDREPENGER, LocalDate.of(2019, 8, 26), LocalDate.of(2019, 10, 6));
        OppgittPeriodeEntitet foreldrepenger2 = lagPeriode(UttakPeriodeType.FORELDREPENGER, LocalDate.of(2019, 10, 7), LocalDate.of(2020, 1, 20));

        var oppgittePerioder = List.of(foreldrepenger2, foreldrepenger1);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, foreldrepenger1.getFom(), null);

        assertThat(justertePerioder).hasSize(1);

        OppgittPeriodeEntitet justert = justertePerioder.get(0);
        assertThat(justert.getFom()).isEqualTo(foreldrepenger1.getFom());
        assertThat(justert.getTom()).isEqualTo(foreldrepenger2.getTom());
    }

    @Test
    public void skal_slå_sammen_like_perioder_gradering() {
        OppgittPeriodeEntitet gradering1 = lagGradering(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20), BigDecimal.TEN);
        OppgittPeriodeEntitet gradering2 = lagGradering(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21), BigDecimal.TEN);

        var oppgittePerioder = List.of(gradering1, gradering2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, gradering1.getFom(), gradering2.getFom());

        assertThat(justertePerioder).hasSize(1);
        OppgittPeriodeEntitet justertGradering = justertePerioder.get(0);
        assertThat(justertGradering.getFom()).isEqualTo(gradering1.getFom());
        assertThat(justertGradering.getTom()).isEqualTo(gradering2.getTom());
    }

    @Test
    public void skal_ikke_slå_sammen_gradering_hvis_forskjellig_arbeidsprosent() {
        OppgittPeriodeEntitet gradering1 = lagGradering(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20), BigDecimal.TEN);
        OppgittPeriodeEntitet gradering2 = lagGradering(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21), BigDecimal.ONE);

        var oppgittePerioder = List.of(gradering1, gradering2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, gradering1.getFom(), gradering2.getFom());

        assertThat(justertePerioder).hasSize(2);
    }

    @Test
    public void skal_slå_sammen_like_perioder_avslutter_og_starter_i_en_helg() {
        OppgittPeriodeEntitet mødrekvote1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 3));
        OppgittPeriodeEntitet mødrekvote2 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 4), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote1, mødrekvote2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom());

        assertThat(justertePerioder).hasSize(1);

        OppgittPeriodeEntitet justertMødrekvote1 = justertePerioder.get(0);
        assertThat(justertMødrekvote1.getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(justertMødrekvote1.getTom()).isEqualTo(mødrekvote2.getTom());
    }

    @Test
    public void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_de_to_siste_periodene_ikke_er_flyttbare() {
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 23));
        OppgittPeriodeEntitet utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 26), LocalDate.of(2019, 8, 26), UtsettelseÅrsak.ARBEID);
        OppgittPeriodeEntitet utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 27), LocalDate.of(2019, 8, 27), UtsettelseÅrsak.FERIE);

        var oppgittePerioder = List.of(mødrekvote, utsettelse1, utsettelse2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(0);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(5));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    public void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_periode_ligger_mellom_to_ikke_flyttbare_perioder() {
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 16));
        OppgittPeriodeEntitet utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        OppgittPeriodeEntitet utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));

        var oppgittePerioder = List.of(mødrekvote, utsettelse1, fellesperiode, utsettelse2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(5);
    }

    @Test
    public void skal_fylle_hull_skapt_av_justering_med_siste_flyttbare_periode_før_hullet() {
        OppgittPeriodeEntitet mødrekvote1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        OppgittPeriodeEntitet utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        OppgittPeriodeEntitet mødrekvote2 = lagPeriode(UttakPeriodeType.FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));
        OppgittPeriodeEntitet utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 22), LocalDate.of(2019, 8, 22));

        var oppgittePerioder = List.of(mødrekvote1, utsettelse1, mødrekvote2, fellesperiode, utsettelse2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom().minusDays(7));

        assertThat(justertePerioder).hasSize(6);
        assertThat(justertePerioder.get(4).getFom()).isEqualTo(mødrekvote2.getFom());
        assertThat(justertePerioder.get(4).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(4).getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_hele_perioden_bak_ikke_flyttbar_flyttes_igjennom() {
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote, utsettelse, fellesperiode);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(LocalDate.of(2019, 8, 16));
    }

    @Test
    public void skal_beholde_siste_uttaksdag_ved_fødsel_før_termin_og_hele_siste_perioden_flyttes_gjennom_en_ikke_flyttbar_periode() {
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote, utsettelse, fellesperiode);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void siste_periode_før_utsettelse_blir_fjernet_helt() {
        //mandag
        LocalDate termin = LocalDate.of(2018, 10, 1);

        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(4));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, utsettelse);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusDays(5));

        assertThat(justertePerioder).hasSize(2);

        OppgittPeriodeEntitet fellesperiode = justertePerioder.get(0);
        assertThat(fellesperiode.getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(fellesperiode.getTom()).isEqualTo(mødrekvote.getTom());

        OppgittPeriodeEntitet justertUtsettelse = justertePerioder.get(1);
        assertThat(justertUtsettelse.getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertUtsettelse.getTom()).isEqualTo(utsettelse.getTom());
    }

    @Test
    public void fødsel_før_termin_skal_ikke_skyve_manglende_perioder_i_helger_ut_i_virkedager() {
        //mandag
        LocalDate termin = LocalDate.of(2019, 1, 14);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusWeeks(6).minusDays(3));
        //starter mandag, avslutter mandag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(6), termin.plusWeeks(7));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Føder fredag
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.minusDays(3));

        assertThat(justertePerioder).hasSize(2);

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(0);
        //Skal starte på fredag
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(3));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().minusDays(1));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(1);
        //Skal starte på fredag
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().minusDays(3));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void søknadsperiode_i_helg_og_føder_før() {
        //lørdag
        LocalDate termin = LocalDate.of(2019, 1, 12);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3).plusDays(2), termin.minusDays(1));
        //starter lørdag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(7).minusDays(1));
        //starter lørdag, avslutter fredag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel fredag
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.minusDays(1));

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        //Skal starte på fredag
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(fpff.getTom().minusDays(1));

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(1);
        //Skal starte på fredag
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(1));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().minusDays(1));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(2);
        //Skal starte på fredag
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().minusDays(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void søknadsperiode_i_helg_og_føder_etter() {
        //søndag
        LocalDate termin = LocalDate.of(2019, 1, 13);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3).plusDays(1), termin.minusDays(2));
        //starter søndag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(5));
        //starter søndag, avslutter torsdag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel fredag
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusDays(2));

        assertThat(justertePerioder).hasSize(4);

        OppgittPeriodeEntitet opprettettFellesperiode = justertePerioder.get(0);
        assertThat(opprettettFellesperiode.getFom()).isEqualTo(fpff.getFom());
        assertThat(opprettettFellesperiode.getTom()).isEqualTo(fpff.getFom());

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusDays(1));
        assertThat(justertFpff.getTom()).isEqualTo(fpff.getTom().plusDays(3));

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(2);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().plusDays(2));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().plusDays(3));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusDays(2));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void familehendelse_i_helg_og_føder_før() {
        //torsdag
        LocalDate termin = LocalDate.of(2019, 1, 10);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        //starter torsdag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(1));
        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusDays(4), termin.plusDays(8));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel lørdag
        LocalDate justertFamilehendelse = termin.minusDays(5);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(justertFamilehendelse.minusDays(1));

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(1);
        //mandag til tirsag
        assertThat(justertMødrekvote.getFom()).isEqualTo(justertFamilehendelse.plusDays(2));
        assertThat(justertMødrekvote.getTom()).isEqualTo(justertFamilehendelse.plusDays(3));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(2);
        assertThat(justertFellesperiode.getFom()).isEqualTo(justertFamilehendelse.plusDays(4));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void skal_flytte_igjennom_hull_når_fødsel_er_etter_familehendelse() {
        //mandag
        LocalDate termin = LocalDate.of(2019, 1, 14);
        //starter mandag, avslutter torsdag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(3));
        //hull fom fredag tom tirsdag
        //starter onsdag, avslutter fredag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(1).plusDays(2), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Fødsel onsdag
        LocalDate justertFamilehendelse = termin.plusDays(2);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(4);

        OppgittPeriodeEntitet mødrekvoteFørHull = justertePerioder.get(1);
        //onsdag til torsdag
        assertThat(mødrekvoteFørHull.getFom()).isEqualTo(justertFamilehendelse);
        assertThat(mødrekvoteFørHull.getTom()).isEqualTo(justertFamilehendelse.plusDays(1));

        //fredag til tirsdag fortsatt hull

        OppgittPeriodeEntitet mødrekvoteEtterHull = justertePerioder.get(2);
        //onsdag til torsdag resten av mødrekvoten
        assertThat(mødrekvoteEtterHull.getFom()).isEqualTo(justertFamilehendelse.plusWeeks(1));
        assertThat(mødrekvoteEtterHull.getTom()).isEqualTo(justertFamilehendelse.plusWeeks(1).plusDays(1));

        //fredag til fredag
        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusDays(2));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void skal_flytte_igjennom_hull_når_fødsel_er_før_familehendelse() {
        //mandag
        LocalDate termin = LocalDate.of(2019, 1, 14);
        //starter mandag, avslutter torsdag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(3));
        //hull fom fredag tom tirsdag
        //starter onsdag, avslutter fredag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(1).plusDays(2), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Fødsel torsdag
        LocalDate justertFamilehendelse = termin.minusDays(4);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet mødrekvoteFørHull = justertePerioder.get(0);
        //torsdag til tirsdag
        assertThat(mødrekvoteFørHull.getFom()).isEqualTo(justertFamilehendelse);
        assertThat(mødrekvoteFørHull.getTom()).isEqualTo(justertFamilehendelse.plusDays(5));

        OppgittPeriodeEntitet fellesperiodeFørHull = justertePerioder.get(1);
        //onsdag til torsdag
        assertThat(fellesperiodeFørHull.getFom()).isEqualTo(justertFamilehendelse.plusDays(6));
        assertThat(fellesperiodeFørHull.getTom()).isEqualTo(justertFamilehendelse.plusDays(7));

        //fredag til tirsdag fortsatt hull

        //onsdag til fredag
        OppgittPeriodeEntitet fellesperiodeEtterHull = justertePerioder.get(2);
        assertThat(fellesperiodeEtterHull.getFom()).isEqualTo(justertFamilehendelse.plusWeeks(2).minusDays(1));
        assertThat(fellesperiodeEtterHull.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void familehendelse_i_helg_og_føder_etter() {
        //torsdag
        LocalDate termin = LocalDate.of(2019, 1, 10);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        //starter torsdag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(1));
        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusDays(4), termin.plusDays(8));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel lørdag
        LocalDate justertFamilehendelse = termin.plusDays(2);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(4);

        OppgittPeriodeEntitet opprettetFellesperiode = justertePerioder.get(0);
        //torsdag til søndag
        assertThat(opprettetFellesperiode.getFom()).isEqualTo(fpff.getFom());
        //fjerner helg fra slutten
        assertThat(opprettetFellesperiode.getTom()).isEqualTo(fpff.getFom().plusDays(3).minusDays(2));

        OppgittPeriodeEntitet justertFPFF = justertePerioder.get(1);
        assertThat(justertFPFF.getFom()).isEqualTo(fpff.getFom().plusDays(4));
        //fredag
        assertThat(justertFPFF.getTom()).isEqualTo(justertFamilehendelse.minusDays(1));

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(2);

        //mandag
        assertThat(justertMødrekvote.getFom()).isEqualTo(justertFamilehendelse.plusDays(2));
        assertThat(justertMødrekvote.getTom()).isEqualTo(justertFamilehendelse.plusDays(3));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(justertFamilehendelse.plusDays(4));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void fødsel_etter_termin_skal_ikke_skyve_manglende_perioder_i_helger_ut_i_virkedager() {
        //mandag
        LocalDate termin = LocalDate.of(2018, 12, 10);

        //starter mandag, avslutter fredag
        OppgittPeriodeEntitet mødrekvote = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusWeeks(7).minusDays(3));
        //starter mandag, avslutter mandag
        OppgittPeriodeEntitet fellesperiode1 = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(7), termin.plusWeeks(8));
        //Denne skal bli borte
        OppgittPeriodeEntitet fellesperiode2 = lagPeriode(UttakPeriodeType.FELLESPERIODE, termin.plusWeeks(8).plusDays(1), termin.plusWeeks(8).plusDays(1));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode1, fellesperiode2);

        //Føder en dag før termin
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusDays(1));

        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet justertMødrekvote = justertePerioder.get(1);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().plusDays(1));
        //avslutte mandag
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().plusDays(3));

        OppgittPeriodeEntitet justertFellesperiode = justertePerioder.get(2);
        //avslutte mandag
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode1.getFom().plusDays(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode1.getTom().plusDays(1));
    }

    @Test
    public void foreldrepenger_før_fødsel_forsvinner_når_fødsel_er_4_uker_for_tidlig() {
        LocalDate fødselsdato = LocalDate.of(2019, 1, 10);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder en dag før termin
        LocalDate nyFødselsdato = fødselsdato.minusDays(28);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, nyFødselsdato);

        //Periode skal flyttes 1 dag tidligere, men ikke før første uttak
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(1);
        OppgittPeriodeEntitet justertMk = justertePerioder.get(0);
        assertThat(justertMk.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(nyFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(mk.getTom());
    }

    @Test
    public void sen_fødsel_der_familiehendelse_flyttes_innad_i_hull() {
        LocalDate termin = LocalDate.of(2018, 10, 1);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(3));
        //hull mellom fpff og mødrekvote
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin.plusWeeks(1), termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder to dager etter termin. Familehendelsedato blir flyttet innad i hullet
        //Skal føre til at det ikke blir en endring i søknadsperiodene
        LocalDate faktiskFødselsdato = termin.plusDays(4);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(2);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom());

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    public void sen_fødsel_der_familiehendelse_flyttes_fra_ett_hull_til_et_annet() {
        LocalDate termin = LocalDate.of(2019, 1, 15);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2018, 12, 24), LocalDate.of(2019, 1, 11));
        //hull mellom fpff og mødrekvote 14 - 16
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 1 ,17), LocalDate.of(2019, 1 ,18));
        //hull mellom mødrekvote og fellesperiode 21 - 22
        OppgittPeriodeEntitet fellesperiode = lagPeriode(UttakPeriodeType.FELLESPERIODE, LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 25));
        var oppgittePerioder = List.of(fpff, mk, fellesperiode);

        //Føder en uke etter termin. Hopper over mødrekvote og inn i et annet hull
        LocalDate faktiskFødselsdato = LocalDate.of(2019, 1, 21);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(5);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(LocalDate.of(2018, 12, 24));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(LocalDate.of(2018, 12, 25));

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(LocalDate.of(2018, 12, 26));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(LocalDate.of(2019, 1, 11));

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(LocalDate.of(2019, 1, 17));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(LocalDate.of(2019, 1, 18));

        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(LocalDate.of(2019, 1, 23));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(LocalDate.of(2019, 1, 24));

        assertThat(justertePerioder.get(4).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertePerioder.get(4).getFom()).isEqualTo(LocalDate.of(2019, 1, 25));
        assertThat(justertePerioder.get(4).getTom()).isEqualTo(LocalDate.of(2019, 1, 25));
    }

    @Test
    public void tidlig_fødsel_der_familiehendelse_flyttes_innad_i_hull() {
        LocalDate termin = LocalDate.of(2018, 10, 1);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusWeeks(2));
        //hull mellom fpff og mødrekvote
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin.plusDays(1), termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder 5 dager før termin. Familehendelsedato blir flyttet innad i hullet
        //Skal føre til at det ikke blir en endring i søknadsperiodene
        LocalDate faktiskFødselsdato = termin.minusDays(4);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(2);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom());

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    public void tidlig_fødsel_der_familiehendelse_flyttes_til_ett_i_hull_ingen_virkedager_i_mellom() {
        LocalDate termin = LocalDate.of(2018, 10, 1);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusWeeks(2));
        //hull mellom fpff og mødrekvote
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder 5 dager før termin. Familehendelsedato blir flyttet inn i hullet. Siden det ikke er noen virkdager i mellom termin og hull så skal søknadsperiodene beholdes
        LocalDate faktiskFødselsdato = termin.minusDays(4);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(2);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom());

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    public void sen_fødsel_der_familiehendelse_flyttes_til_en_utsettelse_virkedager_i_mellom() {
        LocalDate termin = LocalDate.of(2019, 1, 14);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2018, 12, 24), LocalDate.of(2019, 1, 11));
        OppgittPeriodeEntitet mødrekvote1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 1, 14), LocalDate.of(2019, 1, 18));
        OppgittPeriodeEntitet utsettelse = lagUtsettelse(LocalDate.of(2019, 1, 21), LocalDate.of(2019, 1, 25));
        OppgittPeriodeEntitet mødrekvote2 = lagPeriode(UttakPeriodeType.MØDREKVOTE, LocalDate.of(2019, 1, 28), LocalDate.of(2019, 1, 29));
        var oppgittePerioder = List.of(fpff, mødrekvote1, utsettelse, mødrekvote2);

        //Familehendelsedato blir flyttet inn i utsettelsen.
        LocalDate faktiskFødselsdato = LocalDate.of(2019, 1, 22);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(4);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(LocalDate.of(2018, 12, 24));
        //fjerner helg fra slutten
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(LocalDate.of(2018, 12, 28));

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(LocalDate.of(2018, 12, 31));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(LocalDate.of(2019, 1, 18));

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(UttakPeriodeType.UDEFINERT);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(utsettelse.getTom());

        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(LocalDate.of(2019, 1, 28));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(LocalDate.of(2019, 1, 29));
    }

    @Test
    public void fyller_på_med_fellesperiode_i_start_ved_fødsel_1_dag_etter_termin() {
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder en dag etter termin
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, fødselsdato.plusDays(1));

        //Periode skal flyttes 1 dag fremover, og det skal fylles på med fellesperiode i starten
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(3);

        OppgittPeriodeEntitet ekstraFp = justertePerioder.get(0);
        assertThat(ekstraFp.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(ekstraFp.getFom()).isEqualTo(fpff.getFom());
        assertThat(ekstraFp.getTom()).isEqualTo(fpff.getFom());

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusDays(1));
        assertThat(justertFpff.getTom()).isEqualTo(fpff.getTom().plusDays(1));

        OppgittPeriodeEntitet justertMk = justertePerioder.get(2);
        assertThat(justertMk.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(mk.getFom().plusDays(1));
        assertThat(justertMk.getTom()).isEqualTo(mk.getTom());
    }

    @Test
    public void utsettelses_skal_ikke_flyttes_dersom_fødsel_før_termin() {
        LocalDate fødselsdato = LocalDate.of(2019, 1, 17);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(10).minusDays(1));
        OppgittPeriodeEntitet utsettelsePgaFerie = lagUtsettelse(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(1));
        OppgittPeriodeEntitet fp = lagPeriode(UttakPeriodeType.FELLESPERIODE, fødselsdato.plusWeeks(14), fødselsdato.plusWeeks(24).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk, utsettelsePgaFerie, fp);


        //Føder en uke før termin
        LocalDate justertFødselsdato = fødselsdato.minusWeeks(1);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        //Periodene skal flyttes 1 uke, utsettelse skal være uendret og fpff skal avkortes.
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(5);

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(justertFødselsdato.minusDays(1));

        OppgittPeriodeEntitet justertMk = justertePerioder.get(1);
        assertThat(justertMk.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(justertFødselsdato.plusWeeks(10).minusDays(1));
        assertThat(justertMk.getArbeidsprosent()).isNull();

        OppgittPeriodeEntitet flyttetFpFørUtsettelse = justertePerioder.get(2);
        assertThat(flyttetFpFørUtsettelse.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(flyttetFpFørUtsettelse.getFom()).isEqualTo(justertFødselsdato.plusWeeks(10));
        assertThat(flyttetFpFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(11).minusDays(1));

        OppgittPeriodeEntitet uendretUtsettelse = justertePerioder.get(3);
        assertThat(uendretUtsettelse.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
        assertThat(uendretUtsettelse.getFom()).isEqualTo(utsettelsePgaFerie.getFom());
        assertThat(uendretUtsettelse.getTom()).isEqualTo(utsettelsePgaFerie.getTom());

        OppgittPeriodeEntitet justertFp = justertePerioder.get(4);
        assertThat(justertFp.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(fp.getFom());
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    public void utsettelses_skal_ikke_flyttes_dersom_fødsel_etter_termin() {
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(10).minusDays(1));
        OppgittPeriodeEntitet utsettelsePgaFerie = lagUtsettelse(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(3));
        OppgittPeriodeEntitet fp = lagPeriode(UttakPeriodeType.FELLESPERIODE, fødselsdato.plusWeeks(14), fødselsdato.plusWeeks(24).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk, utsettelsePgaFerie, fp);


        //Føder en uke etter termin
        LocalDate justertFødselsdato = fødselsdato.plusWeeks(1);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        //Periodene skal flyttes 1 uke, utsettelse skal være uendret og fpff skal avkortes.
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(6);

        OppgittPeriodeEntitet ekstraFp = justertePerioder.get(0);
        assertThat(ekstraFp.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(ekstraFp.getFom()).isEqualTo(fpff.getFom());
        //fjerner helg fra slutten
        assertThat(ekstraFp.getTom()).isEqualTo(fpff.getFom().plusWeeks(1).minusDays(1).minusDays(2));

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusWeeks(1));
        assertThat(justertFpff.getTom()).isEqualTo(justertFødselsdato.minusDays(3));

        OppgittPeriodeEntitet justertMkFørUtsettelse = justertePerioder.get(2);
        assertThat(justertMkFørUtsettelse.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMkFørUtsettelse.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMkFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(9).minusDays(3));

        OppgittPeriodeEntitet uendretUtsettelse = justertePerioder.get(3);
        assertThat(uendretUtsettelse.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
        assertThat(uendretUtsettelse.getFom()).isEqualTo(utsettelsePgaFerie.getFom());
        assertThat(uendretUtsettelse.getTom()).isEqualTo(utsettelsePgaFerie.getTom());

        OppgittPeriodeEntitet justertMkEtterUtsettelse = justertePerioder.get(4);
        assertThat(justertMkEtterUtsettelse.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMkEtterUtsettelse.getFom()).isEqualTo(fødselsdato.plusWeeks(14));
        assertThat(justertMkEtterUtsettelse.getTom()).isEqualTo(fødselsdato.plusWeeks(15).minusDays(3));

        OppgittPeriodeEntitet justertFp = justertePerioder.get(5);
        assertThat(justertFp.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(fødselsdato.plusWeeks(14+1));
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    public void skal_flytte_gjennom_flere_utsettelser_med_helg_i_mellom() {
        //lørdag
        LocalDate fødselsdato = LocalDate.of(2019, 1, 14);
        //mandag
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        OppgittPeriodeEntitet mk1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(3));
        OppgittPeriodeEntitet utsettelse1 = lagUtsettelse(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(7).minusDays(3), UtsettelseÅrsak.FERIE);
        OppgittPeriodeEntitet utsettelse2 = lagUtsettelse(fødselsdato.plusWeeks(7), fødselsdato.plusWeeks(8).minusDays(3), UtsettelseÅrsak.ARBEID);
        OppgittPeriodeEntitet fp = lagPeriode(UttakPeriodeType.FELLESPERIODE, fødselsdato.plusWeeks(8), fødselsdato.plusWeeks(11).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk1, utsettelse1, utsettelse2, fp);

        //Føder en uke før termin
        LocalDate justertFødselsdato = fødselsdato.minusWeeks(1);
        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(6);

        OppgittPeriodeEntitet justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(justertFødselsdato.minusDays(3));

        OppgittPeriodeEntitet justertMk = justertePerioder.get(1);
        assertThat(justertMk.getPeriodeType()).isEqualTo(UttakPeriodeType.MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(justertFødselsdato.plusWeeks(6).minusDays(3));
        assertThat(justertMk.getArbeidsprosent()).isNull();

        OppgittPeriodeEntitet flyttetFpFørUtsettelse = justertePerioder.get(2);
        assertThat(flyttetFpFørUtsettelse.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(flyttetFpFørUtsettelse.getFom()).isEqualTo(justertFødselsdato.plusWeeks(6));
        assertThat(flyttetFpFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(7).minusDays(3));

        OppgittPeriodeEntitet uendretUtsettelse1 = justertePerioder.get(3);
        assertThat(uendretUtsettelse1.getÅrsak()).isEqualTo(UtsettelseÅrsak.FERIE);
        assertThat(uendretUtsettelse1.getFom()).isEqualTo(utsettelse1.getFom());
        assertThat(uendretUtsettelse1.getTom()).isEqualTo(utsettelse1.getTom());

        OppgittPeriodeEntitet uendretUtsettelse2 = justertePerioder.get(4);
        assertThat(uendretUtsettelse2.getÅrsak()).isEqualTo(UtsettelseÅrsak.ARBEID);
        assertThat(uendretUtsettelse2.getFom()).isEqualTo(utsettelse2.getFom());
        assertThat(uendretUtsettelse2.getTom()).isEqualTo(utsettelse2.getTom());

        OppgittPeriodeEntitet justertFp = justertePerioder.get(5);
        assertThat(justertFp.getPeriodeType()).isEqualTo(UttakPeriodeType.FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(justertFødselsdato.plusWeeks(9));
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    public void skal_fjerne_perioder_med_helg() {
        LocalDate termin = LocalDate.of(2019, 2, 23);
        OppgittPeriodeEntitet fpff = lagPeriode(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        OppgittPeriodeEntitet mk1 = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin, termin.plusDays(1));
        OppgittPeriodeEntitet mk2 = lagPeriode(UttakPeriodeType.MØDREKVOTE, termin.plusDays(2), termin.plusWeeks(6));
        var oppgittePerioder = List.of(fpff, mk1, mk2);

        var justertePerioder = justerFordelingTjeneste.juster(oppgittePerioder, termin, termin.plusWeeks(2));

        assertThat(justertePerioder).hasSize(3);
    }

    private OppgittPeriodeEntitet lagPeriode(UttakPeriodeType uttakPeriodeType, LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(uttakPeriodeType)
            .build();
    }

    private OppgittPeriodeEntitet lagUtsettelse(LocalDate fom, LocalDate tom) {
        return lagUtsettelse(fom, tom, UtsettelseÅrsak.FERIE);
    }

    private OppgittPeriodeEntitet lagUtsettelse(LocalDate fom, LocalDate tom, UtsettelseÅrsak årsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(årsak)
            .build();
    }

    private OppgittPeriodeEntitet lagGradering(UttakPeriodeType uttakPeriodeType, LocalDate fom, LocalDate tom, BigDecimal arbeidsprosent) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(uttakPeriodeType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(arbeidsprosent)
            .build();
    }

    /**
     * Sammenligning av eksakt match på perioder(sammenligner bare fom-tom). Vanlig equals har "fuzzy" logikk rundt helg, så den kan ikke brukes i dette tilfellet.
     */
    private boolean likePerioder(List<OppgittPeriodeEntitet> perioder1, List<OppgittPeriodeEntitet> perioder2) {
        if (perioder1.size()!=perioder2.size()) {
            return false;
        }
        for (int i = 0; i<perioder1.size(); i++) {
            OppgittPeriodeEntitet oppgittPeriode1 = perioder1.get(i);
            OppgittPeriodeEntitet oppgittPeriode2 = perioder2.get(i);
            if (!oppgittPeriode1.getFom().equals(oppgittPeriode2.getFom()) || !oppgittPeriode1.getTom().equals(oppgittPeriode2.getTom())) {
                return false;
            }
        }
        return true;
    }

}
