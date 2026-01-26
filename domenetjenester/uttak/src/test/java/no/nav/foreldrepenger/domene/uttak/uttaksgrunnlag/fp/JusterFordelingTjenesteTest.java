package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FERIE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.SYKDOM;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.erHelg;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilMandag;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

class JusterFordelingTjenesteTest {

    @Test
    void normal_case_føder_på_termin() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(15).minusDays(1));
        var fp = lagPeriode(FELLESPERIODE, fødselsdato.plusWeeks(15), fødselsdato.plusWeeks(31).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk, fp);

        //Føder på termin
        var justertePerioder = juster(oppgittePerioder, fødselsdato, fødselsdato);

        //Oppgitteperioder er uendret
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isTrue();
    }

    @Test
    void foreldrepenger_før_fødsel_forkortes_ved_for_tidlig_fødsel() {
        var termindato = LocalDate.of(2019, 1, 1); // Tirsdag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var oppgittePerioder = List.of(fpff);

        //Føder en dag før termin
        var fødselsdato = termindato.minusDays(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        //Periode skal flyttes 1 dag tidligere, men ikke før første uttak
        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst().getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.getFirst().getTom()).isEqualTo(flyttFraHelgTilFredag(fødselsdato.minusDays(1))); // Søndag flyttes til fredag
    }

    @Test
    void foreldrepenger_før_fødsel_forkortes_ved_for_tidlig_fødsel_flere_perioder() {
        var fødselsdato = LocalDate.of(2019, 1, 14);
        var fpff1 = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusWeeks(1));
        //Denne blir helt borte
        var fpff2 = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(1).plusDays(1),
            fødselsdato.minusDays(1));
        var oppgittePerioder = List.of(fpff1, fpff2);

        //En dag igjen til fpff
        var justertePerioder = juster(oppgittePerioder, fødselsdato, fødselsdato.minusWeeks(3).plusDays(1));

        assertThat(justertePerioder).hasSize(1);
        var justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getFom()).isEqualTo(fpff1.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusWeeks(3));
    }

    @Test
    void normal_case_fødsel_7_uker_før_første_opprinnelige_uttaksdato() {
        var termin = LocalDate.of(2021, 8, 11);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(15).minusDays(1));
        var fp = lagPeriode(FELLESPERIODE, termin.plusWeeks(15), termin.plusWeeks(31).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk, fp);

        var fødsel = termin.minusWeeks(10);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(2);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødsel.plusWeeks(15).minusDays(1));
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødsel.plusWeeks(15));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fp.getTom());
    }

    @Test
    void normal_case_fødsel_7_uker_før_første_opprinnelige_uttaksdato_aleneomsorg() {
        var termin = LocalDate.of(2021, 8, 11);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var fp = lagPeriode(FORELDREPENGER, termin, termin.plusWeeks(46).minusDays(1));
        var oppgittePerioder = List.of(fpff, fp);

        var fødsel = termin.minusWeeks(10);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fp.getTom());
    }

    /**
     * F: FORELDREPENGER, U: utsettelse
     *              ---|FFFFUU
     *      |FFFF           UU   (Etter flytting av perioder)
     *      |FFFFFF         UU   (Siste 2 ukene med F blir fylt på med riktig kvote)
     *      |FFFFFFFFFF          (Fyller perioden etter dette med siste justerbare periode)
     *      |FFFFFFFFFFFFFFFUU   (Fyller opprinnelige søkte periode) = Resultat
     */
    @Test
    void fødsel_før_termin_aleneomsorg_hvor_bruker_søkt_om_utsettelse_innenfor_ukene_forbeholdt_mor_fylles_med_foreldrepenger_og_utsettelse_beholds_etter_termin() {
        var termin = LocalDate.of(2021, 8, 11);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var fp = lagPeriode(FORELDREPENGER, termin, termin.plusWeeks(4).minusDays(1));
        var utsettelse = lagUtsettelse(termin.plusWeeks(4), termin.plusWeeks(6).minusDays(1));
        var oppgittePerioder = List.of(fpff, fp, utsettelse);

        var fødsel = termin.minusWeeks(10);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(2);
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødsel);

        // Perioden mellom fødsel og fødsel + 6 uker fylles med FORELDREPENGER
        // Perioden mellom 6 uker etter fødsl og frem til termin fylles med FORELDREPENGER
        // Perioden etter termin som var i opprinnelig søknad fylles med siste flyttbare periode (FORELDREPENGER)
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fp.getTom());

        assertThat(justertePerioder.get(1)).isEqualTo(utsettelse);
    }



    @Test
    void antall_virkedager_for_periode_skal_være_det_samme_etter_justering() {
        //mandag
        var termin = LocalDate.of(2018, 10, 1);

        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(4));
        var utsettelse = lagUtsettelse(termin.plusWeeks(1), termin.plusWeeks(2).minusDays(3));
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(2), termin.plusWeeks(3));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode, utsettelse);

        var justertePerioder = juster(oppgittePerioder, termin, termin.plusDays(4));

        assertThat(justertePerioder).hasSize(5);

        var justertMødrekvoteFørSplitt = justertePerioder.get(1);
        assertThat(justertMødrekvoteFørSplitt.getFom()).isEqualTo(LocalDate.of(2018, 10, 5));
        assertThat(justertMødrekvoteFørSplitt.getTom()).isEqualTo(LocalDate.of(2018, 10, 5));

        var justertUtsettelse = justertePerioder.get(2);
        assertThat(justertUtsettelse.getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertUtsettelse.getTom()).isEqualTo(utsettelse.getTom());

        var justertMødrekvoteEtterSplitt = justertePerioder.get(3);
        assertThat(justertMødrekvoteEtterSplitt.getFom()).isEqualTo(LocalDate.of(2018, 10, 15));
        assertThat(justertMødrekvoteEtterSplitt.getTom()).isEqualTo(LocalDate.of(2018, 10, 18));

        var justertFellesperiode = justertePerioder.get(4);
        assertThat(justertFellesperiode.getFom()).isEqualTo(LocalDate.of(2018, 10, 19));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void siste_periode_blir_fjernet_helt() {
        //mandag
        var termin = LocalDate.of(2018, 10, 1);

        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(4));

        var oppgittePerioder = List.of(mødrekvote);

        var justertePerioder = juster(oppgittePerioder, termin, termin.plusDays(10));

        assertThat(justertePerioder).hasSize(1);

        var fellesperiode = justertePerioder.get(0);
        assertThat(fellesperiode.getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(fellesperiode.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_siste_periode_er_ikke_flyttbar() {
        var mødrekvote = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 23));
        var utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 26), LocalDate.of(2019, 8, 26));

        var oppgittePerioder = List.of(mødrekvote, utsettelse);

        var justertePerioder = juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(2);

        var justertMødrekvote = justertePerioder.get(0);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(5));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    void skal_slå_sammen_like_perioder() {
        var mødrekvote1 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        var mødrekvote2 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        var mødrekvote3 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));
        var utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 22), LocalDate.of(2019, 8, 22));
        var mødrekvote4 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 23), LocalDate.of(2019, 8, 23));

        var oppgittePerioder = List.of(mødrekvote1, mødrekvote2, mødrekvote3, utsettelse, mødrekvote4);

        var justertePerioder = juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom());

        assertThat(justertePerioder).hasSize(3);

        var justertMødrekvote1 = justertePerioder.get(0);
        assertThat(justertMødrekvote1.getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(justertMødrekvote1.getTom()).isEqualTo(mødrekvote3.getTom());
    }

    @Test
    void justering_av_periode_delvis_forbi_ikke_flyttbar_periode_skal_ikke_føre_til_hull_etter_denne_perioden() {
        var termin = LocalDate.of(2024, 5, 1);

        var ffff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var mødrekvote1 = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(8).minusDays(1));
        // hull 2 uker
        var gradering = lagGradering(FELLESPERIODE, termin.plusWeeks(15), termin.plusWeeks(25).minusDays(1), BigDecimal.valueOf(3.75));
        var mødrekvote2 = lagPeriode(MØDREKVOTE, termin.plusWeeks(25), termin.plusWeeks(27).minusDays(1));
        // hull
        var mødrekvote3 = lagPeriode(MØDREKVOTE, termin.plusWeeks(35), termin.plusWeeks(36).minusDays(1));
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(36), termin.plusWeeks(37));

        var oppgittePerioder = List.of(
            ffff,
            mødrekvote1,
            gradering,
            mødrekvote2,
            mødrekvote3,
            fellesperiode
        );

        var fødsel = termin.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(5);
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(ffff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødsel.minusDays(1));

        // 1 av de 3 ukene av mødrekvoten som låg etter gradering skal flyttes før
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødsel.plusWeeks(9).minusDays(1));

        assertThat(justertePerioder.get(2)).isEqualTo(gradering);

        // 2 av de 3 ukene av mødrekvoten som låg etter gradering skal ikke flyttes forbi
        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(gradering.getTom().plusDays(1));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(gradering.getTom().plusWeeks(2));

        assertThat(justertePerioder.get(4).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(4).getFom()).isEqualTo(mødrekvote3.getFom());
        assertThat(justertePerioder.get(4).getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void ikke_skal_slå_sammen_perioder_der_mottatt_dato_er_ulik() {
        var mottattDato1 = LocalDate.of(2020, 1, 1);
        var mødrekvote1 = OppgittPeriodeBuilder.ny().medPeriode(LocalDate.of(2019, 8, 15), LocalDate.of(2019, 8, 19))
            .medPeriodeType(MØDREKVOTE)
            .medMottattDato(mottattDato1)
            .build();
        var mottattDato2 = LocalDate.of(2020, 2, 2);
        var mødrekvote2 = OppgittPeriodeBuilder.ny().medPeriode(LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 25))
            .medPeriodeType(MØDREKVOTE)
            .medMottattDato(mottattDato2)
            .build();

        var oppgittePerioder = List.of(mødrekvote1, mødrekvote2);

        var justertePerioder = juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom());

        assertThat(justertePerioder).hasSize(2);

        var justertMødrekvote1 = justertePerioder.get(0);
        var justertMødrekvote2 = justertePerioder.get(1);
        assertThat(justertMødrekvote1.getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(justertMødrekvote1.getTom()).isEqualTo(mødrekvote1.getTom());
        assertThat(justertMødrekvote2.getFom()).isEqualTo(mødrekvote2.getFom());
        assertThat(justertMødrekvote2.getTom()).isEqualTo(mødrekvote2.getTom());
    }

    @Test
    void skal_slå_sammen_like_perioder_usortert() {
        var foreldrepenger1 = lagPeriode(FORELDREPENGER, LocalDate.of(2019, 8, 26), LocalDate.of(2019, 10, 6));
        var foreldrepenger2 = lagPeriode(FORELDREPENGER, LocalDate.of(2019, 10, 7), LocalDate.of(2020, 1, 20));

        var oppgittePerioder = List.of(foreldrepenger2, foreldrepenger1);

        var justertePerioder = juster(oppgittePerioder, foreldrepenger1.getFom(), null);

        assertThat(justertePerioder).hasSize(1);

        var justert = justertePerioder.get(0);
        assertThat(justert.getFom()).isEqualTo(foreldrepenger1.getFom());
        assertThat(justert.getTom()).isEqualTo(foreldrepenger2.getTom());
    }

    @Test
    void skal_slå_sammen_like_perioder_gradering() {
        var gradering1 = lagGradering(MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20), BigDecimal.TEN);
        var gradering2 = lagGradering(MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21), BigDecimal.TEN);

        var oppgittePerioder = List.of(gradering1, gradering2);

        var justertePerioder = juster(oppgittePerioder, gradering1.getFom(), gradering2.getFom());

        assertThat(justertePerioder).hasSize(1);
        var justertGradering = justertePerioder.get(0);
        assertThat(justertGradering.getFom()).isEqualTo(gradering1.getFom());
        assertThat(justertGradering.getTom()).isEqualTo(gradering2.getTom());
    }

    @Test
    void gradert_fellesperiode_som_havner_inne_i_ny_peridoer_forbehold_foreldrepenger_før_fødsel_skal_erstattes_med_FFF_med_redusert_utbetaling() {
        var termin = LocalDate.of(2024,8, 14);
        var fødsel = termin.minusWeeks(2);
        var gradertFellesperiodeFørFFF = lagGradering(FELLESPERIODE, termin.minusWeeks(10), termin.minusWeeks(3).minusDays(1), BigDecimal.TEN);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var oppgittePerioder = List.of(gradertFellesperiodeFørFFF, fpff);

        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(gradertFellesperiodeFørFFF.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødsel.minusWeeks(3).minusDays(1));

        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødsel.minusWeeks(3));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(gradertFellesperiodeFørFFF.getTom());
        assertThat(justertePerioder.get(1).isGradert()).isTrue();

        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fødsel.minusDays(1));
        assertThat(justertePerioder.get(2).isGradert()).isFalse();

        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(fpff.getTom());
    }

    @Test
    void gradert_foreldrepenger_som_havner_inne_i_ny_peridoer_forbehold_foreldrepenger_før_fødsel_skal_erstattes_med_FFF_med_redusert_utbetaling() {
        var termin = LocalDate.of(2024,8, 14);
        var fødsel = termin.minusWeeks(2);
        var gradertForeldrepengerFørFFF = lagGradering(FORELDREPENGER, termin.minusWeeks(10), termin.minusWeeks(3).minusDays(1), BigDecimal.TEN);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var oppgittePerioder = List.of(gradertForeldrepengerFørFFF, fpff);

        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(gradertForeldrepengerFørFFF.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødsel.minusWeeks(3).minusDays(1));

        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødsel.minusWeeks(3));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(gradertForeldrepengerFørFFF.getTom());
        assertThat(justertePerioder.get(1).isGradert()).isTrue();

        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fødsel.minusDays(1));
        assertThat(justertePerioder.get(2).isGradert()).isFalse();

        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(fpff.getTom());
    }

    @Test
    void skal_ikke_slå_sammen_gradering_hvis_forskjellig_arbeidsprosent() {
        var gradering1 = lagGradering(MØDREKVOTE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20), BigDecimal.TEN);
        var gradering2 = lagGradering(MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21), BigDecimal.ONE);

        var oppgittePerioder = List.of(gradering1, gradering2);

        var justertePerioder = juster(oppgittePerioder, gradering1.getFom(), gradering2.getFom());

        assertThat(justertePerioder).hasSize(2);
    }

    @Test
    void skal_slå_sammen_like_perioder_avslutter_og_starter_i_en_helg() {
        var mødrekvote1 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 3));
        var mødrekvote2 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 4), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote1, mødrekvote2);

        var justertePerioder = juster(oppgittePerioder, mødrekvote1.getFom(), mødrekvote1.getFom());

        assertThat(justertePerioder).hasSize(1);

        var justertMødrekvote1 = justertePerioder.get(0);
        assertThat(justertMødrekvote1.getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(justertMødrekvote1.getTom()).isEqualTo(mødrekvote2.getTom());
    }

    @Test
    void ingen_flyttbare_perioder_fører_til_hull_og_skal_ikke_fylles_ved_fødsel_før_termin() {
        var termin = LocalDate.of(2019, 8, 20);
        var gradering = lagGradering(MØDREKVOTE, termin, termin.plusWeeks(15).minusDays(1), BigDecimal.TEN);

        var oppgittePerioder = List.of(gradering);

        var fødsel = termin.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst()).isEqualTo(gradering);
    }

    @Test
    void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_de_to_siste_periodene_ikke_er_flyttbare() {
        var mødrekvote = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 23));
        var utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 26), LocalDate.of(2019, 8, 26), ARBEID);
        var utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 27), LocalDate.of(2019, 8, 27), FERIE);

        var oppgittePerioder = List.of(mødrekvote, utsettelse1, utsettelse2);

        var justertePerioder = juster(oppgittePerioder, mødrekvote.getFom(), mødrekvote.getFom().minusDays(5));

        assertThat(justertePerioder).hasSize(3);

        var justertMødrekvote = justertePerioder.get(0);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(5));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_periode_ligger_mellom_to_ikke_flyttbare_perioder() {
        var mødrekvote = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 16));
        var utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        var fellesperiode = lagPeriode(FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        var utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));

        var oppgittePerioder = List.of(mødrekvote, utsettelse1, fellesperiode, utsettelse2);

        var termin = mødrekvote.getFom();
        var fødselsdato = termin.minusDays(5);
        var justertePerioder = juster(oppgittePerioder, termin, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote.getTom()); // Fyller hull som oppstår innenfor de 6 første ukene etter termin hvis det er søkt om.
        // Beholder ikke justerbare perioder etter termin
        assertThat(justertePerioder.get(1)).isEqualTo(utsettelse1);
        assertThat(justertePerioder.get(2)).isEqualTo(fellesperiode);
        assertThat(justertePerioder.get(3)).isEqualTo(utsettelse2);
    }

    @Test
    void skal_fylle_hull_skapt_innenfor_ukene_forbeholdt_mor_med_flyttbar_mødrekvote() {
        var mødrekvote1 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        var utsettelse1 = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        var mødrekvote2 = lagPeriode(FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));
        var fellesperiode = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 21));
        var utsettelse2 = lagUtsettelse(LocalDate.of(2019, 8, 22), LocalDate.of(2019, 8, 22));

        var oppgittePerioder = List.of(mødrekvote1, utsettelse1, mødrekvote2, fellesperiode, utsettelse2);

        var termin = mødrekvote1.getFom();
        var fødselsdato = termin.minusDays(7);
        var justertePerioder = juster(oppgittePerioder, termin, fødselsdato);

        assertThat(justertePerioder).hasSize(5);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote1.getTom()); // Fremdeles innenfor de 6 første ukene etter fødsel forbeholdt mor
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);

        assertThat(justertePerioder.get(1)).isEqualTo(utsettelse1);
        assertThat(justertePerioder.get(2)).isEqualTo(mødrekvote2);
        assertThat(justertePerioder.get(3)).isEqualTo(fellesperiode);
        assertThat(justertePerioder.get(4)).isEqualTo(utsettelse2);
    }

    @Test
    void skal_ikke_lage_hull_hvis_fødsel_før_termin_og_hele_perioden_bak_ikke_flyttbar_flyttes_igjennom() {
        var mødrekvote = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        var utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        var fellesperiode = lagPeriode(FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote, utsettelse, fellesperiode);

        var fødselsdato = mødrekvote.getFom().minusDays(5);
        var justertePerioder = juster(oppgittePerioder, mødrekvote.getFom(), fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote.getTom()); // Fredag

        assertThat(justertePerioder.get(1)).isEqualTo(utsettelse);
        assertThat(justertePerioder.get(2)).isEqualTo(fellesperiode);
    }

    // 	  ---|mmmmmmmmMMMMMMff
	//    |mmmmmmmmff MMMMMM
	//    |mmmmmmmmfffMMMMMMff
    @Test
    void hull_som_oppstår_etter_ukene_forbehold_mor_skal_fylles_med_siste_flyttbare_periode_og_ikke_segmentet_som_overlapper() {
        var termin = LocalDate.of(2024, 4, 17); // onsdag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(10).minusDays(1));
        var gradertMødrekvote = lagGradering(MØDREKVOTE, termin.plusWeeks(10), termin.plusWeeks(20).minusDays(1), BigDecimal.TEN);
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(20), termin.plusWeeks(22).minusDays(1));

        var oppgittePerioder = List.of(mødrekvote, gradertMødrekvote, fellesperiode);
        var fødsel = termin.minusWeeks(3); // lenger enn fellesperioden
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        var dagerMellomFødselOgTermin = Math.abs(ChronoUnit.DAYS.between(fødsel, termin));
        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote.getTom().minusDays(dagerMellomFødselOgTermin));

        var forventetStartdatoFellesperiode = mødrekvote.getTom().minusDays(dagerMellomFødselOgTermin).plusDays(1);
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(forventetStartdatoFellesperiode);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(gradertMødrekvote.getFom().minusDays(1));

        assertThat(justertePerioder.get(2)).isEqualTo(gradertMødrekvote);
        assertThat(justertePerioder.get(3)).isEqualTo(fellesperiode);
    }




    @Test
    void skal_beholde_siste_uttaksdag_ved_fødsel_før_termin_og_hele_siste_perioden_flyttes_gjennom_en_ikke_flyttbar_periode() {
        var mødrekvote = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 8, 16), LocalDate.of(2019, 8, 16));
        var utsettelse = lagUtsettelse(LocalDate.of(2019, 8, 19), LocalDate.of(2019, 8, 19));
        var fellesperiode = lagPeriode(FELLESPERIODE, LocalDate.of(2019, 8, 20), LocalDate.of(2019, 8, 20));

        var oppgittePerioder = List.of(mødrekvote, utsettelse, fellesperiode);

        var fødselsdato = mødrekvote.getFom().minusDays(5);
        var justertePerioder = juster(oppgittePerioder, mødrekvote.getFom(), fødselsdato);

        assertThat(justertePerioder).hasSize(3);

        assertThat(justertePerioder.get(0).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote.getTom()); // Fredag
        assertThat(justertePerioder.get(1)).isEqualTo(utsettelse);
        assertThat(justertePerioder.get(2)).isEqualTo(fellesperiode);
    }

    @Test
    void siste_periode_før_utsettelse_blir_fjernet_helt() {
        //mandag
        var termin = LocalDate.of(2018, 10, 1);

        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(4));
        var utsettelse = lagUtsettelse(termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, utsettelse);

        var justertePerioder = juster(oppgittePerioder, termin, termin.plusDays(5));

        assertThat(justertePerioder).hasSize(2);

        var fellesperiode = justertePerioder.get(0);
        assertThat(fellesperiode.getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(fellesperiode.getTom()).isEqualTo(mødrekvote.getTom());

        var justertUtsettelse = justertePerioder.get(1);
        assertThat(justertUtsettelse.getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertUtsettelse.getTom()).isEqualTo(utsettelse.getTom());
    }

    @Test
    void fødsel_før_termin_skal_ikke_skyve_manglende_perioder_i_helger_ut_i_virkedager() {
        //mandag
        var termin = LocalDate.of(2019, 1, 14);

        //starter mandag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(6).minusDays(3));
        //starter mandag, avslutter mandag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(6), termin.plusWeeks(7));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Føder fredag
        var justertePerioder = juster(oppgittePerioder, termin, termin.minusDays(3));

        assertThat(justertePerioder).hasSize(2);

        var justertMødrekvote = justertePerioder.get(0);
        //Skal starte på fredag
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().minusDays(3));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().minusDays(1));

        var justertFellesperiode = justertePerioder.get(1);
        //Skal starte på fredag
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().minusDays(3));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void søknadsperiode_i_helg_og_føder_før() {
        //lørdag
        var termin = LocalDate.of(2019, 1, 12);

        //starter mandag, avslutter fredag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3).plusDays(2), termin.minusDays(1));
        //starter lørdag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(7).minusDays(1));
        //starter lørdag, avslutter fredag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel fredag
        var fødsel = termin.minusDays(1);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(3);
        var justertFpff = justertePerioder.get(0);
        var justertMødrekvote = justertePerioder.get(1);
        var justertFellesperiode = justertePerioder.get(2);

        //Skal starte på fredag
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(fødsel.minusDays(1));

        //Skal starte på fredag
        assertThat(justertMødrekvote.getFom()).isEqualTo(fødsel);
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());

        // Fellesperioden skal ikke justeres til venstre. Siden fellesperiode starter i utgangspunktet på en lørdag så endres fom til førstkommende mandag
        assertThat(justertFellesperiode.getFom()).isEqualTo(Virkedager.justerHelgTilMandag(fellesperiode.getFom()));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void søknadsperiode_i_helg_og_føder_etter() {
        //søndag
        var termin = LocalDate.of(2019, 1, 13);

        //starter mandag, avslutter fredag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3).plusDays(1), termin.minusDays(2));
        //starter søndag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(5));
        //starter søndag, avslutter torsdag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(1), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel fredag
        var justertePerioder = juster(oppgittePerioder, termin, termin.plusDays(2));

        assertThat(justertePerioder).hasSize(4);

        var opprettettFellesperiode = justertePerioder.get(0);
        assertThat(opprettettFellesperiode.getFom()).isEqualTo(fpff.getFom());
        assertThat(opprettettFellesperiode.getTom()).isEqualTo(fpff.getFom());

        var justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusDays(1));
        assertThat(justertFpff.getTom()).isEqualTo(fpff.getTom().plusDays(3));

        var justertMødrekvote = justertePerioder.get(2);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().plusDays(2));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().plusDays(3));

        var justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusDays(2));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void familehendelse_i_helg_og_føder_før() {
        //torsdag
        var termin = LocalDate.of(2019, 1, 10);

        //starter mandag, avslutter fredag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        //starter torsdag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(1));
        //starter mandag, avslutter fredag
        // hull
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusDays(4), termin.plusDays(8));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel lørdag
        var fødsel = termin.minusDays(5);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(3);

        var justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(fødsel.minusDays(1));

        var justertMødrekvote = justertePerioder.get(1);
        //mandag til tirsag
        assertThat(justertMødrekvote.getFom()).isEqualTo(Virkedager.justerHelgTilMandag(fødsel));
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom()); // Innenfor ukene forbehold mor etter fødsel

        var justertFellesperiode = justertePerioder.get(2);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void skal_flytte_igjennom_hull_når_fødsel_er_etter_familehendelse() {
        //mandag
        var termin = LocalDate.of(2019, 1, 14);
        //starter mandag, avslutter torsdag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(3));
        //hull fom fredag tom tirsdag
        //starter onsdag, avslutter fredag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(1).plusDays(2), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Fødsel onsdag
        var justertFamilehendelse = termin.plusDays(2);
        var justertePerioder = juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(4);

        var mødrekvoteFørHull = justertePerioder.get(1);
        //onsdag til torsdag
        assertThat(mødrekvoteFørHull.getFom()).isEqualTo(justertFamilehendelse);
        assertThat(mødrekvoteFørHull.getTom()).isEqualTo(justertFamilehendelse.plusDays(1));

        //fredag til tirsdag fortsatt hull

        var mødrekvoteEtterHull = justertePerioder.get(2);
        //onsdag til torsdag resten av mødrekvoten
        assertThat(mødrekvoteEtterHull.getFom()).isEqualTo(justertFamilehendelse.plusWeeks(1));
        assertThat(mødrekvoteEtterHull.getTom()).isEqualTo(justertFamilehendelse.plusWeeks(1).plusDays(1));

        //fredag til fredag
        var justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusDays(2));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void skal_flytte_igjennom_hull_når_fødsel_er_før_familehendelse() {
        //mandag
        var termin = LocalDate.of(2019, 1, 14);
        //starter mandag, avslutter torsdag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(3));
        //hull fom fredag tom tirsdag
        //starter onsdag, avslutter fredag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusWeeks(1).plusDays(2), termin.plusWeeks(1).plusDays(4));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        //Fødsel torsdag
        var justertFamilehendelse = termin.minusDays(4);
        var justertePerioder = juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(2);
        var justertMødrekvote = justertePerioder.get(0);
        var justertFellesperiode = justertePerioder.get(1);

        // Justere perioden og fyller frem til mødrekvote tom (reste ikke søkt om)
        assertThat(justertMødrekvote.getFom()).isEqualTo(justertFamilehendelse);
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom()); // Er innenfor ukene forbehold mor. Skal ikke justeres lenger inn.
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void familehendelse_i_helg_og_føder_etter() {
        //torsdag
        var termin = LocalDate.of(2019, 1, 10);

        //starter mandag, avslutter fredag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        //starter torsdag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusDays(1));
        //starter mandag, avslutter fredag
        var fellesperiode = lagPeriode(FELLESPERIODE, termin.plusDays(4), termin.plusDays(8));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Fødsel lørdag
        var justertFamilehendelse = termin.plusDays(2);
        var justertePerioder = juster(oppgittePerioder, termin, justertFamilehendelse);

        assertThat(justertePerioder).hasSize(4);

        var opprettetFellesperiode = justertePerioder.get(0);
        //torsdag til søndag
        assertThat(opprettetFellesperiode.getFom()).isEqualTo(fpff.getFom());
        //fjerner helg fra slutten
        assertThat(opprettetFellesperiode.getTom()).isEqualTo(fpff.getFom().plusDays(3).minusDays(2));

        var justertFPFF = justertePerioder.get(1);
        assertThat(justertFPFF.getFom()).isEqualTo(fpff.getFom().plusDays(4));
        //fredag
        assertThat(justertFPFF.getTom()).isEqualTo(justertFamilehendelse.minusDays(1));

        var justertMødrekvote = justertePerioder.get(2);

        //mandag
        assertThat(justertMødrekvote.getFom()).isEqualTo(justertFamilehendelse.plusDays(2));
        assertThat(justertMødrekvote.getTom()).isEqualTo(justertFamilehendelse.plusDays(3));

        var justertFellesperiode = justertePerioder.get(3);
        assertThat(justertFellesperiode.getFom()).isEqualTo(justertFamilehendelse.plusDays(4));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void fødsel_etter_termin_skal_ikke_skyve_manglende_perioder_i_helger_ut_i_virkedager() {
        //mandag
        var termin = LocalDate.of(2018, 12, 10);

        //starter mandag, avslutter fredag
        var mødrekvote = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(7).minusDays(3));
        //starter mandag, avslutter mandag
        var fellesperiode1 = lagPeriode(FELLESPERIODE, termin.plusWeeks(7), termin.plusWeeks(8));
        //Denne skal bli borte
        var fellesperiode2 = lagPeriode(FELLESPERIODE, termin.plusWeeks(8).plusDays(1),
            termin.plusWeeks(8).plusDays(1));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode1, fellesperiode2);

        //Føder en dag før termin
        var justertePerioder = juster(oppgittePerioder, termin, termin.plusDays(1));

        assertThat(justertePerioder).hasSize(3);

        var justertMødrekvote = justertePerioder.get(1);
        assertThat(justertMødrekvote.getFom()).isEqualTo(mødrekvote.getFom().plusDays(1));
        //avslutte mandag
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom().plusDays(3));

        var justertFellesperiode = justertePerioder.get(2);
        //avslutte mandag
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode1.getFom().plusDays(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode1.getTom().plusDays(1));
    }

    @Test
    void foreldrepenger_før_fødsel_forsvinner_når_fødsel_er_4_uker_for_tidlig() {
        var fødselsdato = LocalDate.of(2019, 1, 10);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder en dag før termin
        var nyFødselsdato = fødselsdato.minusDays(28);
        var justertePerioder = juster(oppgittePerioder, fødselsdato, nyFødselsdato);

        //Periode skal flyttes 1 dag tidligere, men ikke før første uttak
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(1);
        var justertMk = justertePerioder.get(0);
        assertThat(justertMk.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(nyFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void sen_fødsel_der_familiehendelse_flyttes_innad_i_hull() {
        var termin = LocalDate.of(2018, 10, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(3));
        //hull mellom fpff og mødrekvote
        var mk = lagPeriode(MØDREKVOTE, termin.plusWeeks(1), termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder to dager etter termin. Familehendelsedato blir flyttet innad i hullet
        //Skal føre til at det ikke blir en endring i søknadsperiodene
        var faktiskFødselsdato = termin.plusDays(4);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(2);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom());

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void sen_fødsel_der_familiehendelse_flyttes_fra_ett_hull_til_et_annet() {
        var termin = LocalDate.of(2019, 1, 15);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2018, 12, 24), LocalDate.of(2019, 1, 11));
        //hull mellom fpff og mødrekvote 14 - 16
        var mk = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 1, 17), LocalDate.of(2019, 1, 18));
        //hull mellom mødrekvote og fellesperiode 21 - 22
        var fellesperiode = lagPeriode(FELLESPERIODE, LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 25));
        var oppgittePerioder = List.of(fpff, mk, fellesperiode);

        //Føder en uke etter termin. Hopper over mødrekvote og inn i et annet hull
        var faktiskFødselsdato = LocalDate.of(2019, 1, 21);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(5);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(LocalDate.of(2018, 12, 24));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(LocalDate.of(2018, 12, 25));

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(LocalDate.of(2018, 12, 26));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(LocalDate.of(2019, 1, 11));

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(LocalDate.of(2019, 1, 17));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(LocalDate.of(2019, 1, 18));

        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(LocalDate.of(2019, 1, 23));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(LocalDate.of(2019, 1, 24));

        assertThat(justertePerioder.get(4).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(4).getFom()).isEqualTo(LocalDate.of(2019, 1, 25));
        assertThat(justertePerioder.get(4).getTom()).isEqualTo(LocalDate.of(2019, 1, 25));
    }

    /**
     *   -  | mmmmmmm
     *   -|mm mmmmmmm
     */
    @Test
    void tidlig_fødsel_der_familiehendelse_flyttes_innad_i_hull_hvor_hull_før_fødsel_fylles_og_hull_etter_beholdes() {
        var termin = LocalDate.of(2018, 10, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusWeeks(2));
        // Hull 2 uker mellom FFF og Termin
        // Hull 1 uke mellom termin og mødrekvote
        var mk = lagPeriode(MØDREKVOTE, termin.plusWeeks(1), termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        // Føder 1 uke før termin. Familehendelsedato blir flyttet innad i hullet
        var faktiskFødselsdato = termin.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(3);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom());

        // Bevarer hull mellom fff og fødsel
        // Bevarer IKKE hull etter fødsel, men før termin

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(faktiskFødselsdato);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(minusVirkedag(termin));

        // Bevarer hull etter termin

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void hull_hvor_foreldrepenger_før_fødsel_er_eneste_tidligere_periode_fylles_med_mødrekvoten_fra_perioden_etter_og_ikke_FFF_som_eneste_periode_før_hullet() {
        var termin = LocalDate.of(2018, 10, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        // Hull 1 uke mellom termin og mødrekvote
        var mk = lagPeriode(MØDREKVOTE, termin.plusWeeks(1), termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        // Føder 1 uke før termin. Familehendelsedato blir flyttet innad i hullet
        // Skal føre til at det ikke blir en endring i søknadsperiodene
        var faktiskFødselsdato = termin.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(3);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(flyttFraHelgTilFredag(faktiskFødselsdato.minusDays(1)));

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(flyttFraHelgTilMandag(faktiskFødselsdato));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(flyttFraHelgTilFredag(termin.minusDays(1)));

        // Hull 1 uke mellom termin og opprinnelig mødrekvote beholdes

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(mk.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void tidlig_fødsel_der_familiehendelse_flyttes_til_ett_i_hull_ingen_virkedager_i_mellom_skal_behold_dager_og_skyve_tilsvarende() {
        var termin = LocalDate.of(2018, 10, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusWeeks(2));
        //hull mellom fpff og mødrekvote
        var mk = lagPeriode(MØDREKVOTE, termin, termin.plusWeeks(7).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder 5 dager før termin. Familehendelsedato blir flyttet inn i hullet. Siden det ikke er noen virkdager i mellom termin og hull så skal søknadsperiodene beholdes
        var faktiskFødselsdato = termin.minusDays(4);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(2);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fpff.getTom()); // Skal beholde hull før fødsel

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(faktiskFødselsdato); // Skal ikke beholde hull etter fødsel som havner innenfor de ukene forbehold mor
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void sen_fødsel_der_familiehendelse_flyttes_til_en_utsettelse_virkedager_i_mellom() {
        var termin = LocalDate.of(2019, 1, 14);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2018, 12, 24), LocalDate.of(2019, 1, 11));
        var mødrekvote1 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 1, 14), LocalDate.of(2019, 1, 18));
        var utsettelse = lagUtsettelse(LocalDate.of(2019, 1, 21), LocalDate.of(2019, 1, 25));
        var mødrekvote2 = lagPeriode(MØDREKVOTE, LocalDate.of(2019, 1, 28), LocalDate.of(2019, 1, 29));
        var oppgittePerioder = List.of(fpff, mødrekvote1, utsettelse, mødrekvote2);

        //Familehendelsedato blir flyttet inn i utsettelsen.
        var faktiskFødselsdato = LocalDate.of(2019, 1, 22);
        var justertePerioder = juster(oppgittePerioder, termin, faktiskFødselsdato);

        assertThat(justertePerioder).hasSize(4);

        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(LocalDate.of(2018, 12, 24));
        //fjerner helg fra slutten
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(LocalDate.of(2018, 12, 28));

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(LocalDate.of(2018, 12, 31));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(LocalDate.of(2019, 1, 18));

        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(UttakPeriodeType.UDEFINERT);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(utsettelse.getTom());

        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(LocalDate.of(2019, 1, 28));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(LocalDate.of(2019, 1, 29));
    }

    @Test
    void fyller_på_med_fellesperiode_i_start_ved_fødsel_1_dag_etter_termin() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder en dag etter termin
        var justertePerioder = juster(oppgittePerioder, fødselsdato, fødselsdato.plusDays(1));

        //Periode skal flyttes 1 dag fremover, og det skal fylles på med fellesperiode i starten
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(3);

        var ekstraFp = justertePerioder.get(0);
        assertThat(ekstraFp.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(ekstraFp.getFom()).isEqualTo(fpff.getFom());
        assertThat(ekstraFp.getTom()).isEqualTo(fpff.getFom());

        var justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusDays(1));
        assertThat(justertFpff.getTom()).isEqualTo(fpff.getTom().plusDays(1));

        var justertMk = justertePerioder.get(2);
        assertThat(justertMk.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(mk.getFom().plusDays(1));
        assertThat(justertMk.getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void skal_arve_mottatt_dato_fra_første_periode_på_fellesperiode_som_er_fylt_på_før_fødsel() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var fpff = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
            .medPeriodeType(FORELDREPENGER_FØR_FØDSEL)
            .medMottattDato(fødselsdato.minusWeeks(4))
            .build();
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk);

        //Føder en dag etter termin
        var justertePerioder = juster(oppgittePerioder, fødselsdato, fødselsdato.plusDays(1));

        var ekstraFp = justertePerioder.get(0);
        assertThat(ekstraFp.getMottattDato()).isEqualTo(fpff.getMottattDato());
    }

    @Test
    void utsettelses_skal_ikke_flyttes_dersom_fødsel_før_termin() {
        var fødselsdato = LocalDate.of(2019, 1, 17);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(10).minusDays(1));
        var utsettelsePgaFerie = lagUtsettelse(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(1));
        var fp = lagPeriode(FELLESPERIODE, fødselsdato.plusWeeks(14), fødselsdato.plusWeeks(24).minusDays(1));
        var oppgittePerioder = List.of(fpff, mk, utsettelsePgaFerie, fp);


        //Føder en uke før termin
        var justertFødselsdato = fødselsdato.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        //Periodene skal flyttes 1 uke, utsettelse skal være uendret og fpff skal avkortes.
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(5);

        var justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(justertFødselsdato.minusDays(1));

        var justertMk = justertePerioder.get(1);
        assertThat(justertMk.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(justertFødselsdato.plusWeeks(10).minusDays(1));
        assertThat(justertMk.getArbeidsprosent()).isNull();

        var flyttetFpFørUtsettelse = justertePerioder.get(2);
        assertThat(flyttetFpFørUtsettelse.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(flyttetFpFørUtsettelse.getFom()).isEqualTo(justertFødselsdato.plusWeeks(10));
        assertThat(flyttetFpFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(11).minusDays(1));

        var uendretUtsettelse = justertePerioder.get(3);
        assertThat(uendretUtsettelse.getÅrsak()).isEqualTo(FERIE);
        assertThat(uendretUtsettelse.getFom()).isEqualTo(utsettelsePgaFerie.getFom());
        assertThat(uendretUtsettelse.getTom()).isEqualTo(utsettelsePgaFerie.getTom());

        var justertFp = justertePerioder.get(4);
        assertThat(justertFp.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(fp.getFom());
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    void utsettelses_skal_ikke_flyttes_dersom_fødsel_etter_termin() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(10).minusDays(1));
        var utsettelsePgaFerie = lagUtsettelse(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(14).minusDays(3));
        var fp = lagPeriode(FELLESPERIODE, fødselsdato.plusWeeks(14), fødselsdato.plusWeeks(24).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk, utsettelsePgaFerie, fp);


        //Føder en uke etter termin
        var justertFødselsdato = fødselsdato.plusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        //Periodene skal flyttes 1 uke, utsettelse skal være uendret og fpff skal avkortes.
        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(6);

        var ekstraFp = justertePerioder.get(0);
        assertThat(ekstraFp.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(ekstraFp.getFom()).isEqualTo(fpff.getFom());
        //fjerner helg fra slutten
        assertThat(ekstraFp.getTom()).isEqualTo(fpff.getFom().plusWeeks(1).minusDays(1).minusDays(2));

        var justertFpff = justertePerioder.get(1);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom().plusWeeks(1));
        assertThat(justertFpff.getTom()).isEqualTo(justertFødselsdato.minusDays(3));

        var justertMkFørUtsettelse = justertePerioder.get(2);
        assertThat(justertMkFørUtsettelse.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMkFørUtsettelse.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMkFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(9).minusDays(3));

        var uendretUtsettelse = justertePerioder.get(3);
        assertThat(uendretUtsettelse.getÅrsak()).isEqualTo(FERIE);
        assertThat(uendretUtsettelse.getFom()).isEqualTo(utsettelsePgaFerie.getFom());
        assertThat(uendretUtsettelse.getTom()).isEqualTo(utsettelsePgaFerie.getTom());

        var justertMkEtterUtsettelse = justertePerioder.get(4);
        assertThat(justertMkEtterUtsettelse.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMkEtterUtsettelse.getFom()).isEqualTo(fødselsdato.plusWeeks(14));
        assertThat(justertMkEtterUtsettelse.getTom()).isEqualTo(fødselsdato.plusWeeks(15).minusDays(3));

        var justertFp = justertePerioder.get(5);
        assertThat(justertFp.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(fødselsdato.plusWeeks(14 + 1));
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    void skal_flytte_gjennom_flere_utsettelser_med_helg_i_mellom() {
        //lørdag
        var fødselsdato = LocalDate.of(2019, 1, 14);
        //mandag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk1 = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(6).minusDays(3));
        var utsettelse1 = lagUtsettelse(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(7).minusDays(3), FERIE);
        var utsettelse2 = lagUtsettelse(fødselsdato.plusWeeks(7), fødselsdato.plusWeeks(8).minusDays(3), ARBEID);
        var fp = lagPeriode(FELLESPERIODE, fødselsdato.plusWeeks(8), fødselsdato.plusWeeks(11).minusDays(3));
        var oppgittePerioder = List.of(fpff, mk1, utsettelse1, utsettelse2, fp);

        //Føder en uke før termin
        var justertFødselsdato = fødselsdato.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, fødselsdato, justertFødselsdato);

        assertThat(likePerioder(oppgittePerioder, justertePerioder)).isFalse();
        assertThat(justertePerioder).hasSize(6);

        var justertFpff = justertePerioder.get(0);
        assertThat(justertFpff.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFpff.getTom()).isEqualTo(flyttFraHelgTilFredag(justertFødselsdato.minusDays(1)));

        var justertMk = justertePerioder.get(1);
        assertThat(justertMk.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMk.getFom()).isEqualTo(justertFødselsdato);
        assertThat(justertMk.getTom()).isEqualTo(justertFødselsdato.plusWeeks(6).minusDays(3));
        assertThat(justertMk.getArbeidsprosent()).isNull();

        var flyttetFpFørUtsettelse = justertePerioder.get(2);
        assertThat(flyttetFpFørUtsettelse.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(flyttetFpFørUtsettelse.getFom()).isEqualTo(justertFødselsdato.plusWeeks(6));
        assertThat(flyttetFpFørUtsettelse.getTom()).isEqualTo(justertFødselsdato.plusWeeks(7).minusDays(3));

        var uendretUtsettelse1 = justertePerioder.get(3);
        assertThat(uendretUtsettelse1.getÅrsak()).isEqualTo(FERIE);
        assertThat(uendretUtsettelse1.getFom()).isEqualTo(utsettelse1.getFom());
        assertThat(uendretUtsettelse1.getTom()).isEqualTo(utsettelse1.getTom());

        var uendretUtsettelse2 = justertePerioder.get(4);
        assertThat(uendretUtsettelse2.getÅrsak()).isEqualTo(ARBEID);
        assertThat(uendretUtsettelse2.getFom()).isEqualTo(utsettelse2.getFom());
        assertThat(uendretUtsettelse2.getTom()).isEqualTo(utsettelse2.getTom());

        var justertFp = justertePerioder.get(5);
        assertThat(justertFp.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertFp.getFom()).isEqualTo(justertFødselsdato.plusWeeks(9));
        assertThat(justertFp.getTom()).isEqualTo(fp.getTom());
    }

    @Test
    void skal_fjerne_perioder_med_helg() {
        var termin = LocalDate.of(2019, 2, 23);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termin.minusWeeks(3), termin.minusDays(1));
        var mk1 = lagPeriode(MØDREKVOTE, termin, termin.plusDays(1));
        var mk2 = lagPeriode(MØDREKVOTE, termin.plusDays(2), termin.plusWeeks(6));
        var oppgittePerioder = List.of(fpff, mk1, mk2);

        var justertePerioder = juster(oppgittePerioder, termin, termin.plusWeeks(2));

        assertThat(justertePerioder).hasSize(3);
    }

    @Test
    void skal_justere_mødrekvote_med_samtidig_uttak() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var termin = fødselsdato;
        var mk = lagSamtidigUttak(MØDREKVOTE, termin.plusWeeks(15), termin.plusWeeks(20), 10);
        var oppgittePerioder = List.of(mk);

        var fødsel = termin.plusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termin, fødsel);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst().getPeriodeType()).isEqualTo(oppgittePerioder.getFirst().getPeriodeType());
        assertThat(justertePerioder.getFirst().getFom()).isEqualTo(flyttFraHelgTilMandag(fødsel));
        assertThat(justertePerioder.getFirst().getTom()).isEqualTo(oppgittePerioder.getFirst().getTom());
    }

    @Test //Unntak: Skal justere mødrekvote
    void ikke_justere_fellesperiode_med_samtidig_uttak() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato.minusWeeks(3), fødselsdato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, fødselsdato, fødselsdato.plusWeeks(15).minusDays(1));
        var fpMedSamtidigUttak = lagSamtidigUttak(FELLESPERIODE, fødselsdato.plusWeeks(15), fødselsdato.plusWeeks(20), 10);
        var oppgittePerioder = List.of(fpff, mk, fpMedSamtidigUttak);

        var justertePerioder = juster(oppgittePerioder, fødselsdato, fødselsdato.plusWeeks(1));

        //3 perioder + Fellesperiode før fødsel
        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fpMedSamtidigUttak.getFom());
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(fpMedSamtidigUttak.getTom());
    }

    @Test
    void ikke_fylle_hull_når_hele_siste_periode_skyves_igjen_hullet_ved_fødsel_før_termin() {
        var termindato = LocalDate.of(2022, 6, 7);
        var fødselsdato = termindato.minusWeeks(2);
        var fpff = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.minusWeeks(3), termindato.minusDays(1))
            .medPeriodeType(FORELDREPENGER_FØR_FØDSEL)
            .build();
        var mk1 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato, termindato.plusWeeks(10))
            .medPeriodeType(MØDREKVOTE)
            .build();
        //Hull
        var mk2 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(12), termindato.plusWeeks(13))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var oppgittePerioder = List.of(fpff, mk1, mk2);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk1.getTom());

        assertThat(justertePerioder.get(2).getFom()).isEqualTo(mk2.getFom());
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk2.getTom());
    }

    @Test
    void skal_fylle_hull_de_første_6_ukene_ved_fødsel_etter_termin() {
        var termindato = LocalDate.of(2022, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(6).minusDays(1));
        //Hull fra uke 6
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to dag etter termin
        var fødselsdato = termindato.plusDays(2);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvote = justertePerioder.get(2);
        var justertFellesperiode = justertePerioder.get(3);
        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    /**
     * ---|---.---  -   ------.-----
     * ----.---|------| -.-----.-----
     */
    @Test
    void skal_fylle_alle_hull_som_oppstår_innefor_periode_etter_fødsel_forbeholdt_mor_fødsel_etter_termin() {
        var termindato = LocalDate.of(2023, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(6).minusDays(1));
        //Hull fra uke 6-7
        var mødrekvoteEtterHull = lagPeriode(MØDREKVOTE, termindato.plusWeeks(7), termindato.plusWeeks(8).minusDays(1));
        // Hull fra uke 8-11
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(11), termindato.plusWeeks(21).minusDays(1));
        // Hull fra uke 12-21
        var mødrekvoteEtterFellesperiode = lagPeriode(MØDREKVOTE, termindato.plusWeeks(21), termindato.plusWeeks(29).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, mødrekvoteEtterHull, fellesperiode, mødrekvoteEtterFellesperiode);

        //Føder to dag etter termin
        var fødselsdato = termindato.plusWeeks(4);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(6);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvoteForbeholdtMor = justertePerioder.get(2);
        var justertMødrekvoteEtterHull = justertePerioder.get(3);
        var justertFellesperiode = justertePerioder.get(4);
        var justertMødrekvoteSlutt = justertePerioder.get(5);

        assertThat(fellesperiodeFørFødsel.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvoteForbeholdtMor.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMødrekvoteForbeholdtMor.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvoteForbeholdtMor.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertMødrekvoteEtterHull.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMødrekvoteEtterHull.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertMødrekvoteEtterHull.getTom()).isEqualTo(fellesperiode.getFom().plusWeeks(1).minusDays(1));

        assertThat(justertFellesperiode.getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusWeeks(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());

        assertThat(justertMødrekvoteSlutt.getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertMødrekvoteSlutt.getFom()).isEqualTo(mødrekvoteEtterFellesperiode.getFom());
        assertThat(justertMødrekvoteSlutt.getTom()).isEqualTo(mødrekvoteEtterFellesperiode.getTom());
    }

    /**
     * f   ---|mmmmmmmmmm
     * - |mmmmmmmmmm
     */
    @Test
    void skal_fylle_alle_hull_før_termin_og_mellom_fødsel_som_er_forbehold_mor_ved_fødsel_før_termin() {
        var termindato = LocalDate.of(2023, 11, 9);
        var fellesperiodeStart = lagPeriode(FELLESPERIODE, termindato.minusWeeks(6), termindato.minusWeeks(5).minusDays(1));
        // Hull T-5 til T-3
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(10).minusDays(1));
        var oppgittePerioder = List.of(fellesperiodeStart, fpff, mødrekvote);

        //Føder to dag etter termin
        var fødselsdato = termindato.minusWeeks(5);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(2);
        var justertFFF = justertePerioder.get(0);
        var justertMødrekvote = justertePerioder.get(1);

        assertThat(justertFFF.getFom()).isEqualTo(fellesperiodeStart.getFom());
        assertThat(justertFFF.getTom()).isEqualTo(fellesperiodeStart.getTom());
        assertThat(justertFFF.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvote.getTom());
        assertThat(justertMødrekvote.getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    /**
     *  ---|mmm     ffffffffff
     *  |mmm        ffffffffff (justering .. bare flytting.. ikke legge til noe nytt)
     *  |mmmmmm     ffffffffff (spesialregel.. legg til noe hun i utgangspunktet har søkt)
     */
    @Test
    void fellesperiode_skal_ikke_flyttes_mer_inn_periode_forbeholdt_mor_etter_fødsel_men_fylles_etterpå() {
        var termindato = LocalDate.of(2023, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(3).minusDays(1));
        // Hull 3 til 10
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(20).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to dag etter termin
        var fødselsdato = termindato.minusWeeks(3);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(2);
        var justertMødrekvot = justertePerioder.get(0);
        var justertFellesperiode = justertePerioder.get(1);

        assertThat(justertMødrekvot.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvot.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    /**
     *  ---|mm     ffffffffff
     *  -|mmmm     ffffffffff (resultat fra justering)
     *  Uttaksreglene vil legge inn manglende søkt periode her =>
     *  -|mmmmXX   ffffffffff (resultat fastsetting av uttaksperioder)
     */
    @Test
    void skal_skyves_inn_i_hull_forbeholdt_mor_men_ikke_fylle_der_hvor_mor_ikke_har_søkt() {
        var termindato = LocalDate.of(2023, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(2).minusDays(1));
        // Hull 3 til 10
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(20).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to dag etter termin
        var fødselsdato = termindato.minusWeeks(2);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        var justertFPFF = justertePerioder.get(0);
        var justertMødrekvot = justertePerioder.get(1);
        var justertFellesperiode = justertePerioder.get(2);

        assertThat(justertFPFF.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFPFF.getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertFPFF.getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);

        assertThat(justertMødrekvot.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvot.getTom())
            .isEqualTo(mødrekvote.getTom())
            .isNotEqualTo(fødselsdato.plusWeeks(6).minusDays(1));
        assertThat(justertMødrekvot.getPeriodeType()).isEqualTo(MØDREKVOTE);

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertFellesperiode.getPeriodeType()).isEqualTo(FELLESPERIODE);
    }

    /**
     *  ---|mmmmffffff
     *  --|mmmmmffffff (fyller med mødrekvote. Fellesperiode flyttes ikke på selv om den er innenfor de 6 første ukene)
     */
    @Test
    void fellesperiode_som_allerede_er_i_periode_forbehold_mor_etter_fødsel_justeres_ikke_men_skal_ikke_flyttes_lenger_inn() {
        var termindato = LocalDate.of(2023, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(4).minusDays(1));
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(4), termindato.plusWeeks(10).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to dag etter termin
        var fødselsdato = termindato.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        var justertFFF = justertePerioder.get(0);
        var justertMødrekvot = justertePerioder.get(1);
        var justertFellesperiode = justertePerioder.get(2);

        assertThat(justertFFF.getFom()).isEqualTo(fpff.getFom());
        assertThat(justertFFF.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvot.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvot.getTom()).isEqualTo(mødrekvote.getTom());

        assertThat(justertFellesperiode).isEqualTo(fellesperiode);
    }


    @Test
    void skal_fylle_hull_forbehold_mor_etter_fødsel_og_skyve_rest_forbi_når_det_er_flere_mødrekvoter_og_fellesperiode_etter_fødsel_reduseres() {
        var termindato = LocalDate.of(2022, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(7).minusDays(1));
        //Hull fra uke 7-10
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(20).minusDays(1));
        var mødrekvoteEtter = lagPeriode(MØDREKVOTE, termindato.plusWeeks(20), termindato.plusWeeks(28).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode, mødrekvoteEtter);

        //Føder to dag etter termin
        var fødselsdato = termindato.plusWeeks(2);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(6);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvoteFørHull = justertePerioder.get(2);
        var justertMødrekvoteEtterHull = justertePerioder.get(3);
        var justertFellesperiode = justertePerioder.get(4);
        var justertMødrekvoteSlutt = justertePerioder.get(5);

        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvoteFørHull.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvoteFørHull.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertMødrekvoteEtterHull.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertMødrekvoteEtterHull.getTom()).isEqualTo(fellesperiode.getFom().plusWeeks(1).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusWeeks(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());

        assertThat(justertMødrekvoteSlutt.getFom()).isEqualTo(mødrekvoteEtter.getFom());
        assertThat(justertMødrekvoteSlutt.getTom()).isEqualTo(mødrekvoteEtter.getTom());
    }


    @Test
    void skal_fylle_hull_de_første_6_ukene_ved_fødsel_etter_termin_fyller_hele_hullet() {
        var termindato = LocalDate.of(2022, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(6).minusDays(1));
        //Hull fra uke 6
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(7), termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to uker etter termin
        var fødselsdato = termindato.plusWeeks(2);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvote = justertePerioder.get(2);
        var justertFellesperiode = justertePerioder.get(3);
        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusWeeks(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void skal_fylle_hull_de_første_6_ukene_ved_fødsel_etter_termin_mens_resten_skyves_til_etter_oppholdet() {
        var termindato = LocalDate.of(2022, 11, 9);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(7).minusDays(1));
        //Hull fra uke 7-10
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(26).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        //Føder to uker etter termin
        var fødselsdato = termindato.plusWeeks(2);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(5);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvote = justertePerioder.get(2);
        var justertMødrekvoteEtterOpphold = justertePerioder.get(3);
        var justertFellesperiode = justertePerioder.get(4);

        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertMødrekvoteEtterOpphold.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertMødrekvoteEtterOpphold.getTom()).isEqualTo(fellesperiode.getFom().plusWeeks(1).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom().plusWeeks(1));
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void skal_fylle_hull_de_første_6_ukene_ved_fødsel_etter_termin_men_bevare_eventuelle_utsettesler_eller_ikke_søkte_perioder() {
        var termindato = LocalDate.of(2023, 1, 4); // mandag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvoteFørUtsettelse = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(2).minusDays(1));
        var utsettelse = lagUtsettelse(termindato.plusWeeks(2), termindato.plusWeeks(3).minusDays(1));
        var mødrekvoteEtterUtsettelse = lagPeriode(MØDREKVOTE, termindato.plusWeeks(3), termindato.plusWeeks(4).minusDays(1));
        // hull fra uke 4-5
        var mødrekvoteEtterHull = lagPeriode(MØDREKVOTE, termindato.plusWeeks(5), termindato.plusWeeks(6).minusDays(1));
        //Hull fra uke 6-10
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(10), termindato.plusWeeks(26).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvoteFørUtsettelse, utsettelse, mødrekvoteEtterUtsettelse, mødrekvoteEtterHull, fellesperiode);

        //Føder to uker etter termin
        var fødselsdato = termindato.plusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(7);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvoteFørUtsettelse = justertePerioder.get(2);
        var justertUtsettelse = justertePerioder.get(3);
        var justertmødrekvoteEtterUtsettelse = justertePerioder.get(4);
        var justertmødrekvoteEtterHull = justertePerioder.get(5);
        var justertFellesperiode = justertePerioder.get(6);

        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvoteFørUtsettelse.getFom()).isEqualTo(Virkedager.plusVirkedager(mødrekvoteFørUtsettelse.getFom(), 5));
        assertThat(justertMødrekvoteFørUtsettelse.getTom()).isEqualTo(utsettelse.getFom().minusDays(1));

        assertThat(justertUtsettelse.getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertUtsettelse.getTom()).isEqualTo(utsettelse.getTom());

        assertThat(justertmødrekvoteEtterUtsettelse.getFom()).isEqualTo(utsettelse.getTom().plusDays(1));
        assertThat(justertmødrekvoteEtterUtsettelse.getTom()).isEqualTo(termindato.plusWeeks(4).minusDays(1)); // Frem til hull

        assertThat(justertmødrekvoteEtterHull.getFom()).isEqualTo(mødrekvoteEtterHull.getFom()); // Starter etter hull
        assertThat(justertmødrekvoteEtterHull.getTom()).isEqualTo(fødselsdato.plusWeeks(6).minusDays(1));

        assertThat(justertFellesperiode.getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertFellesperiode.getTom()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    void fellesperiode_blir_spist_opp_fordi_perioden_mellom_fødsel_og_termin_er_større_enn_fellesperiode() {
        var termindato = LocalDate.of(2023, 1, 4); // mandag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvoteFørFellesperiode = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(7).minusDays(1));
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(7), termindato.plusWeeks(10).minusDays(1));
        var mødrekvoteEtterFellesperiode = lagPeriode(MØDREKVOTE, termindato.plusWeeks(10), termindato.plusWeeks(14).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvoteFørFellesperiode, fellesperiode, mødrekvoteEtterFellesperiode);

        //Føder to uker etter termin
        var fødselsdato = termindato.plusWeeks(3); // Tilsvarer størrelsen på fellesperioden
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvote = justertePerioder.get(2);

        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvoteEtterFellesperiode.getTom());
    }

    @Test
    void første_fellesperiode_blir_spist_opp_mens_andre_fellesperiode_blir_delvis_spist_opp_slik_at_siste_mødrekvote_ikke_forskyves() {
        var termindato = LocalDate.of(2023, 1, 4); // mandag
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvoteFørFørsteFellesperiode = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(7).minusDays(1));
        var fellesperiode1 = lagPeriode(FELLESPERIODE, termindato.plusWeeks(7), termindato.plusWeeks(8).minusDays(1));
        var mødrekvoteMellomFellesperiode = lagPeriode(MØDREKVOTE, termindato.plusWeeks(8), termindato.plusWeeks(14).minusDays(1));
        var fellesperiode2 = lagPeriode(FELLESPERIODE, termindato.plusWeeks(14), termindato.plusWeeks(20).minusDays(1));
        var mødrekvoteSlutt = lagPeriode(MØDREKVOTE, termindato.plusWeeks(20), termindato.plusWeeks(25).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvoteFørFørsteFellesperiode, fellesperiode1, mødrekvoteMellomFellesperiode, fellesperiode2, mødrekvoteSlutt);

        //Føder to uker etter termin
        var fødselsdato = termindato.plusWeeks(3); // Tilsvarer størrelsen på fellesperioden
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(5);
        var fellesperiodeFørFødsel = justertePerioder.get(0);
        var justertFpff = justertePerioder.get(1);
        var justertMødrekvote = justertePerioder.get(2);
        var justertFellesperiode2 = justertePerioder.get(3);
        var justertMødrekvoteSlutt = justertePerioder.get(4);

        assertThat(fellesperiodeFørFødsel.getFom()).isEqualTo(fpff.getFom());
        assertThat(fellesperiodeFørFødsel.getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertFpff.getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertFpff.getTom()).isEqualTo(fødselsdato.minusDays(1));

        // Fellesperiode i mellom er spist opp (1 uke). Forskyvning er derfor redusert fra 3 til 2 videre til neste fellesperiode.

        assertThat(justertMødrekvote.getFom()).isEqualTo(fødselsdato);
        assertThat(justertMødrekvote.getTom()).isEqualTo(mødrekvoteMellomFellesperiode.getTom().plusWeeks(2));

        assertThat(justertFellesperiode2.getFom()).isEqualTo(fellesperiode2.getFom().plusWeeks(2));
        assertThat(justertFellesperiode2.getTom()).isEqualTo(fellesperiode2.getTom()); // Justert og redusert med 2 uker

        // Fellesperiode spiser opp 2 uker. Ny forskyvning blir da 0.

        assertThat(justertMødrekvoteSlutt.getFom()).isEqualTo(mødrekvoteSlutt.getFom());
        assertThat(justertMødrekvoteSlutt.getTom()).isEqualTo(mødrekvoteSlutt.getTom());


    }

    @Test
    void skal_håndtere_termindato_i_helg_ved_fødsel_før_termin_og_hull_fra_uke_6() {
        //Lørdag
        var termindato = LocalDate.of(2022, 12, 3);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        //Fra mandag
        var fom = LocalDate.of(2022, 12, 5);
        var mødrekvote = lagPeriode(MØDREKVOTE, fom, fom.plusWeeks(6).minusDays(1));
        //Hull fra uke 6
        var fellesperiode = lagPeriode(FELLESPERIODE, fom.plusWeeks(7), fom.plusWeeks(10).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        var fødselsdato = LocalDate.of(2022, 11, 18);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
    }

    @Test
    void skal_håndtere_termindato_i_helg_ved_fødsel_etter_termin_og_hull_fra_uke_6() {
        //Lørdag
        var termindato = LocalDate.of(2022, 12, 3);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        //Fra mandag
        var fom = LocalDate.of(2022, 12, 5);
        var mødrekvote = lagPeriode(MØDREKVOTE, fom, fom.plusWeeks(6).minusDays(1));
        //Hull fra uke 6
        var fellesperiode = lagPeriode(FELLESPERIODE, fom.plusWeeks(7), fom.plusWeeks(10).minusDays(1));
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode);

        var fødselsdato = LocalDate.of(2022, 12, 8);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
    }



    @Test
    void fellesperiode_før_fødsel_fødsel_før_termin() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(12), termindato.minusWeeks(11));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode, fpff, mk);

        var fødselsdato = termindato.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);

    }

    @Test
    void fellesperiode_før_fødsel_fødsel_før_termin_inntil_fpff() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(5), termindato.minusWeeks(3).minusDays(1));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode, fpff, mk);

        var fødselsdato = termindato.minusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void fellesperiode_før_fødsel_fødsel_etter_termin() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(10), termindato.minusWeeks(9));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode, fpff, mk);

        var fødselsdato = termindato.plusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void fellesperiode_før_fødsel_skal_ikke_redusers_ved_fødsel_etter_termin() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(5), termindato.minusWeeks(3).minusDays(1));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode, fpff, mk);

        var fødselsdato = termindato.plusWeeks(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fellesperiode.getTom().plusWeeks(1));
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fpff.getFom().plusWeeks(1));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
    }


    @Test
    void flere_perioder_fellesperiode_før_fødsel_fødsel_før_termin_deler_av_FFF_beholdes() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode1 = lagPeriode(FELLESPERIODE, termindato.minusWeeks(10), termindato.minusWeeks(9).minusDays(1));
        // Hull uke 9-5
        var fellesperiode2 = lagPeriode(FELLESPERIODE, termindato.minusWeeks(5), termindato.minusWeeks(3).minusDays(1));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode1, fellesperiode2, fpff, mk);

        var fødselsdato = termindato.minusWeeks(3);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0)).isEqualTo(fellesperiode1);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fellesperiode2.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mk.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void flere_perioder_fellesperiode_før_fødsel_fødsel_før_termin_hele_fpff_blir_borte_fordi_perioden_havner_i_hull() {
        var termindato = LocalDate.of(2022, 11, 30);
        var fellesperiode1 = lagPeriode(FELLESPERIODE, termindato.minusWeeks(10), termindato.minusWeeks(9).minusDays(1));
        // Hull uke 9-5
        var fellesperiode2 = lagPeriode(FELLESPERIODE, termindato.minusWeeks(5), termindato.minusWeeks(3).minusDays(1));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(fellesperiode1, fellesperiode2, fpff, mk);

        var fødselsdato = termindato.minusWeeks(5);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(2);
        assertThat(justertePerioder.get(0)).isEqualTo(fellesperiode1);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void fellesperiode_før_fødsel_og_fødsel_på_startdato_av_fpff() {
        //Saksnummer 152187602
        var termindato = LocalDate.of(2022, 12, 19);
        var fellesperiode1 = lagPeriode(FELLESPERIODE, LocalDate.of(2022, 11, 7), LocalDate.of(2022, 11, 25));
        var fødselsdato = LocalDate.of(2022, 11, 28);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, fødselsdato, LocalDate.of(2022, 12, 16));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(3));
        var oppgittePerioder = List.of(fellesperiode1, fpff, mk);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(2);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode1.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fellesperiode1.getTom());
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mk.getTom());
    }

    @Test
    void periode_overlapper_med_termindato_fødsel_før_termin() {
        //Saksnummer 152234475
        var termindato = LocalDate.of(2023, 5, 24);
        var fp = lagPeriode(FORELDREPENGER, LocalDate.of(2023, 5, 3), LocalDate.of(2024, 4, 10));
        var fødselsdato = LocalDate.of(2023, 5, 3);
        var oppgittePerioder = List.of(fp);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst()).isEqualTo(fp);
    }

    @Test
    void periode_overlapper_med_termindato_fødsel_etter_termin() {
        var termindato = LocalDate.of(2023, 5, 24);
        var fp = lagPeriode(FORELDREPENGER, LocalDate.of(2023, 5, 3), LocalDate.of(2024, 4, 10));
        var fødselsdato = LocalDate.of(2023, 6, 10);
        var oppgittePerioder = List.of(fp);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst()).isEqualTo(fp);
    }

    @Test
    void bare_en_periode_fpff_som_justeres_bort_skal_kaste_exception() {
        var termindato = LocalDate.of(2023, 8, 30);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2023, 8, 10), termindato.minusDays(1));
        var oppgittePerioder = List.of(fpff);

        var fødselsdato = LocalDate.of(2023, 8, 5);
        assertThatThrownBy(() -> juster(oppgittePerioder, termindato, fødselsdato)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fellesperiode_før_fødsel_og_fødsel_etter_fødsel_skal_ikke_lage_hull() {
        //FAGSYSTEM-307795
        var termindato = LocalDate.of(2023, 12, 14);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(4), termindato.minusWeeks(4));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(10));
        var fødselsdato = LocalDate.of(2024, 1, 3);
        var oppgittePerioder = List.of(fellesperiode, fpff, mødrekvote);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(4);
        assertThat(justertePerioder.getFirst().getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.getFirst().getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.getFirst().getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(termindato.plusWeeks(10));
        assertThat(justertePerioder.get(3).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void overføringsperiode_flyttes_ikke() {
        var termindato = LocalDate.of(2024, 3, 26);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(15), termindato.plusWeeks(31).minusDays(1));
        var overføring = lagOverføring(termindato.plusWeeks(31), termindato.plusWeeks(46).minusDays(1));
        var fødselsdato = LocalDate.of(2024, 2, 26);
        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode, overføring);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(flyttFraHelgTilFredag(fødselsdato.plusWeeks(15).minusDays(1)));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato.plusWeeks(15));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(flyttFraHelgTilFredag(overføring.getFom().minusDays(1)));
        assertThat(justertePerioder.get(2)).isEqualTo(overføring);
    }

    @Test
    void fødselFørTerminJusterePeriodeSomKnekkesOppTilFlereGangerPgaHull() {
        var termindato = LocalDate.of(2024, 3, 9);
        var fødselsdato = LocalDate.of(2024, 2, 27);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(15).minusDays(1));
        var fellesperiode1 = lagPeriode(FELLESPERIODE, LocalDate.of(2024,6,24), LocalDate.of(2024,7,5));
        var fellesperiode2 = lagPeriode(FELLESPERIODE, LocalDate.of(2024,7,22), LocalDate.of(2024,7,26));
        var fellesperiode3 = lagPeriode(FELLESPERIODE, LocalDate.of(2024,8,12), LocalDate.of(2024,10,4));
        var fellesperiode4 = lagPeriode(FELLESPERIODE, LocalDate.of(2024,10,21), LocalDate.of(2024,11,22));

        var oppgittePerioder = List.of(fpff, mødrekvote, fellesperiode1, fellesperiode2, fellesperiode3, fellesperiode4);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSameSizeAs(oppgittePerioder);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(flyttFraHelgTilMandag(fpff.getFom()));
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(flyttFraHelgTilFredag(fødselsdato.minusDays(1)));
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(flyttFraHelgTilFredag(fødselsdato.plusWeeks(15).minusDays(1)));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato.plusWeeks(15)));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fellesperiode1.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(3)).isEqualTo(oppgittePerioder.get(3));
        assertThat(justertePerioder.get(4)).isEqualTo(oppgittePerioder.get(4));
        assertThat(justertePerioder.get(5)).isEqualTo(oppgittePerioder.get(5));
    }



    //   F F|mmmmmmmmmmmmmmm
    //   F |mmmmmmmmmmmmmmmm
    @Test
    void utsettelse_mellom_fødsel_og_termin_skal_ikke_føre_til_at_perioder_som_var_etter_termin_blir_skyvet_forbi_fødsel_pga_utsettelse_ved_fødsel_før_termin() {
        var termindato = LocalDate.of(2024, 3, 13);

        var fpff1 = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusWeeks(2).minusDays(1));
        var utsettelse = lagUtsettelse(termindato.minusWeeks(2), termindato.minusWeeks(1).minusDays(1));
        var fpff2 = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(1), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(12).minusDays(1));

        var fødselsdato = utsettelse.getTom();
        var oppgittePerioder = List.of(fpff1, utsettelse, fpff2, mødrekvote);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);
        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0)).isEqualTo(fpff1);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(utsettelse.getFom());
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mødrekvote.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    /**
     *  ffff       -|mmmmmmmm
     *  ff - |mmmmmmmmmmmmmmm
     *  fff- |mmmmmmmmmmmmmmm (skal fylle uke 3 før fødsel med fellesperiode! Vil da gå ut til manuell behandling og vurdering gjøres av saksbehandler)
     */
    @Test
    void fyller_fra_ventre_til_høyre_med_FFF_og_fyller_resten_med_fellesperiode() {
        var termindato = LocalDate.of(2022, 7,12);
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.minusWeeks(8), termindato.minusWeeks(5).minusDays(1));
        // Hull
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(1), termindato.minusDays(1));
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(12).minusDays(1));

        var fødselsdato = fellesperiode.getTom().plusWeeks(1).minusDays(1);
        var oppgittePerioder = List.of(fellesperiode, fpff, mødrekvote);

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);
        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fellesperiode.getFom());
        assertThat(justertePerioder.get(0).getTom())
            .isEqualTo(flyttFraHelgTilFredag(fødselsdato.minusWeeks(2).minusDays(1)))
            .isNotEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato.minusWeeks(2)));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(2).getFom()).isEqualTo(flyttFraHelgTilMandag(fødselsdato));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(mødrekvote.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }


    //    |mmmm          fffffffff
    // |mmmm  f          ffffffff   (forskyver ikke fellesperiode inn)
    // |mmmmmmf          fffffffff  (fyller mødrekvote og fellesperiode)
    @Test
    void fellesperiode_flyttes_ikke_inn_i_de_første_6_ukene_forbeholdt_mor_og_oppslitting_tar_høyde_for_redusert_forskyvning() {
        var termindato = LocalDate.of(2024, 7, 4);
        var fødseldato = termindato.minusWeeks(3);
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(4).minusDays(1));
        // utsettelse/opphold av en eller annen grunn
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(12), termindato.plusWeeks(20).minusDays(1));

        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        var justertePerioder = juster(oppgittePerioder, termindato, fødseldato);
        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødseldato);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødseldato.plusWeeks(6).minusDays(1)); // Forsyves og fyller hull.

        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FELLESPERIODE);
        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødseldato.plusWeeks(6));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(mødrekvote.getTom());

        assertThat(justertePerioder.get(2)).isEqualTo(fellesperiode);
    }

    // G er gradert fellesperiode, F er fp_før_fødsel:
    //  GGGGGGGGGFFF|mmmmmmmmmmmmmmffffffffffff
    //  GGGGGG|GG                               (G avkortes til dagen før ny familiehendelse)
    //  GGGGGG|mmmmmmmmmmmmmmffffffffffffffffff (juster resten til venstre)
    //  GGGFFF|mmmmmmmmmmmmmmffffffffffffffffff (Legg til FFF til slutt hvor F nå arver egenskapene til G, altså redusert utbetaling)
    @Test
    void fødsel_havner_i_ikke_flyttbar_periode_fører_til_avvkorting_og_gradert_fellesperiode_erstattes_med_FFFF() {
        var termindato = LocalDate.of(2024, 12, 27);
        var fødselsdato = LocalDate.of(2024, 12, 4);
        var fp1 = lagGradering(FELLESPERIODE, termindato.minusWeeks(11),  termindato.minusWeeks(3).minusDays(1), BigDecimal.valueOf(20));
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.minusWeeks(3), termindato.minusDays(1));
        var mk = lagPeriode(MØDREKVOTE, termindato, termindato.plusWeeks(19).minusDays(1));
        var fp2 = lagPeriode(FELLESPERIODE, termindato.plusWeeks(19), termindato.plusWeeks(31));
        var oppgittePerioder = List.of(fp1, fpff, mk, fp2);

        //Føder på termin
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        //Oppgitteperioder er uendret
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(oppgittePerioder.get(0).getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(fødselsdato.minusWeeks(3).minusDays(1));

        assertThat(justertePerioder.get(1).getFom()).isEqualTo(fødselsdato.minusWeeks(3));
        assertThat(justertePerioder.get(1).getTom()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(justertePerioder.get(1).getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
        assertThat(justertePerioder.get(1).isGradert()).isTrue();

        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fødselsdato);
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fødselsdato.plusWeeks(19).minusDays(1));
        assertThat(justertePerioder.get(3).getFom()).isEqualTo(fødselsdato.plusWeeks(19));
        assertThat(justertePerioder.get(3).getTom()).isEqualTo(fp2.getTom());
    }

    @Test
    void FAGSYSTEM_365456_fikser_bug_ved_avkorting_av_fellesperiode_ved_høyrejustering_hvor_det_ble_overlapp_av_fellesperiode() {
        var termindato = LocalDate.of(2024, 12, 3);
        var mødrekvote = lagPeriode(MØDREKVOTE, termindato.minusWeeks(4), termindato.plusWeeks(2).minusDays(1));
        var fellesperiode = lagPeriode(FELLESPERIODE, termindato.plusWeeks(2), termindato.plusWeeks(15).minusDays(1));
        var oppgittePerioder = List.of(mødrekvote, fellesperiode);

        var fødtDagerSenere = 1;
        var fødselsdato = termindato.plusDays(fødtDagerSenere);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødrekvote.getFom()); // Skal fylle hull før fødsel med gyldig kvote som er fellesperiode i dette tilfellet
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(FELLESPERIODE);

        assertThat(justertePerioder.get(fødtDagerSenere).getFom()).isEqualTo(mødrekvote.getFom().plusDays(fødtDagerSenere));
        assertThat(justertePerioder.get(fødtDagerSenere).getTom()).isEqualTo(mødrekvote.getTom().plusDays(fødtDagerSenere));
        assertThat(justertePerioder.get(fødtDagerSenere).getPeriodeType()).isEqualTo(MØDREKVOTE);

        assertThat(justertePerioder.get(2).getFom()).isEqualTo(fellesperiode.getFom().plusDays(fødtDagerSenere));
        assertThat(justertePerioder.get(2).getTom()).isEqualTo(fellesperiode.getTom());
        assertThat(justertePerioder.get(2).getPeriodeType()).isEqualTo(FELLESPERIODE);
    }

    @Test
    void fpff_etter_termin_skal_føre_til_ingen_justering() {
        var termindato = LocalDate.of(2025, 4, 7);
        var fpff = lagPeriode(FORELDREPENGER_FØR_FØDSEL, termindato.plusWeeks(4), termindato.plusWeeks(5));
        var oppgittePerioder = List.of(fpff);

        var fødselsdato = termindato.plusMonths(1);
        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).hasSize(1);
        assertThat(justertePerioder.getFirst().getFom()).isEqualTo(fpff.getFom());
        assertThat(justertePerioder.getFirst().getTom()).isEqualTo(fpff.getTom());
        assertThat(justertePerioder.getFirst().getPeriodeType()).isEqualTo(FORELDREPENGER_FØR_FØDSEL);
    }

    // Fellesperiode justeres ikke inn i de 6 ukene forbeholdt mor etter fødsel. Fylles med mødrekvote.
    // Termin:        |mmmfffff    fffff
    // Fødsel:      |mmmmmfffff    fffff
    @Test
    void papirsøknad_fellesperiode_innenfor_6_ukene_forbeholdt_mor_skal_ikke_forskyves_lenger_inn_i_intervallet() {
        var termindato = LocalDate.of(2025, 6, 18);
        var fødsel = LocalDate.of(2025, 6, 12);
        var mødre = lagPeriode(MØDREKVOTE, LocalDate.of(2025, 6, 18), LocalDate.of(2025, 7, 10));
        var felles1 = lagPeriode(FELLESPERIODE, LocalDate.of(2025, 7, 11), LocalDate.of(2025, 8, 22));
        var felles2 = lagPeriode(FELLESPERIODE, LocalDate.of(2025, 9, 22), LocalDate.of(2026, 4, 22));
        var oppgittePerioder = List.of(mødre, felles1, felles2);

        var justertePerioder = juster(oppgittePerioder, termindato, fødsel);

        assertThat(justertePerioder).hasSize(3);
        assertThat(justertePerioder.get(0).getFom()).isEqualTo(fødsel);
        assertThat(justertePerioder.get(0).getTom()).isEqualTo(mødre.getTom());
        assertThat(justertePerioder.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(justertePerioder.get(1)).isEqualTo(felles1);
        assertThat(justertePerioder.get(2)).isEqualTo(felles2);
    }

    //---|----uu-  --  --   (2 dag forskyvning)
    //  ---|		        (2 dag forskyvning)
    //  ---|--  --		    (2 dag forskyvning, fyller hull)
    //  ---|--uu--		    (2 dag forskyvning)
    //  ---|--uu-- -		(2 dag forskyvning, full forskyvning siden fylte hull er etter starten av denne perioden)
    //  ---|--uu-- --  - 	(1 dag forskyvning, redusert forskyvning tilsvarende fylte hull som er før starten av denne perioden)
    //  ---|--uu-- --  --- 	(1 dag forskyvning)
    @Test
    void mødrekvote_skal_fylle_hull_hvis_innenfor_de_6_første_ukene_ved_justering_og_senere_justering_må_redusere_forskyvning_i_henhold_til_hvor_mye_hullet_er_tettet() {
        var termindato = LocalDate.of(2025, 9, 8);
        var fødselsdato = LocalDate.of(2025, 9, 18);

        var oppgittePerioder = List.of(
            lagPeriode(FORELDREPENGER_FØR_FØDSEL, LocalDate.of(2025, 8, 18), LocalDate.of(2025, 9, 5)),
            lagPeriode(MØDREKVOTE, LocalDate.of(2025, 9, 8), LocalDate.of(2025, 10, 6)),
            lagUtsettelse(LocalDate.of(2025, 10, 7), LocalDate.of(2025, 10, 16), SYKDOM),
            lagUtsettelse(LocalDate.of(2025, 10, 17), LocalDate.of(2025, 10, 17), SYKDOM),
            lagPeriode(MØDREKVOTE, LocalDate.of(2025, 10, 20), LocalDate.of(2025, 10, 24)),
            // hull som delvis fylles med mødrekvote ved justering
            lagPeriode(MØDREKVOTE, LocalDate.of(2025, 10, 30), LocalDate.of(2025, 12, 24))
        );

        var justertePerioder = juster(oppgittePerioder, termindato, fødselsdato);

        assertThat(justertePerioder).isNotEmpty();

    }

    private static List<OppgittPeriodeEntitet> juster(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate familehendelse1, LocalDate familehendelse2) {
        var justert = JusterFordelingTjeneste.justerForFamiliehendelse(oppgittePerioder, familehendelse1, familehendelse2, RelasjonsRolleType.MORA, false);
        return slåSammenLikePerioder(justert);
    }

    static OppgittPeriodeEntitet lagPeriode(UttakPeriodeType uttakPeriodeType, LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny().medPeriode(fom, tom).medPeriodeType(uttakPeriodeType).build();
    }

    static OppgittPeriodeEntitet lagSamtidigUttak(UttakPeriodeType type, LocalDate fom, LocalDate tom, int samtidigUttakProsent) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(type)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(samtidigUttakProsent))
            .medSamtidigUttak(true)
            .build();
    }


    static OppgittPeriodeEntitet lagOverføring(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
    }

    static OppgittPeriodeEntitet lagUtsettelse(LocalDate fom, LocalDate tom) {
        return lagUtsettelse(fom, tom, FERIE);
    }

    static OppgittPeriodeEntitet lagUtsettelse(LocalDate fom, LocalDate tom, UtsettelseÅrsak årsak) {
        return OppgittPeriodeBuilder.ny().medPeriode(fom, tom).medPeriodeType(UttakPeriodeType.UDEFINERT).medÅrsak(årsak).build();
    }

    static OppgittPeriodeEntitet lagGradering(UttakPeriodeType uttakPeriodeType, LocalDate fom, LocalDate tom, BigDecimal arbeidsprosent) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(uttakPeriodeType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(arbeidsprosent)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();
    }

    /**
     * Sammenligning av eksakt match på perioder(sammenligner bare fom-tom). Vanlig equals har "fuzzy" logikk rundt helg, så den kan ikke brukes i dette tilfellet.
     */
    private boolean likePerioder(List<OppgittPeriodeEntitet> perioder1, List<OppgittPeriodeEntitet> perioder2) {
        if (perioder1.size() != perioder2.size()) {
            return false;
        }
        for (var i = 0; i < perioder1.size(); i++) {
            var oppgittPeriode1 = perioder1.get(i);
            var oppgittPeriode2 = perioder2.get(i);
            if (!oppgittPeriode1.getFom().equals(oppgittPeriode2.getFom()) || !oppgittPeriode1.getTom()
                .equals(oppgittPeriode2.getTom())) {
                return false;
            }
        }
        return true;
    }

    private static LocalDate minusVirkedag(LocalDate dato) {
        var resultat = dato;
        var dager = 1;
        while (dager > 0 || erHelg(resultat)) {
            if (!erHelg(resultat)) {
                dager--;
            }
            resultat = resultat.minusDays(1);
        }
        return resultat;
    }

}
