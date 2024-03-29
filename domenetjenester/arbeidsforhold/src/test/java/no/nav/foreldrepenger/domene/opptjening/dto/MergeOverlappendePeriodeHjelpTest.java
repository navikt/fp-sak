package no.nav.foreldrepenger.domene.opptjening.dto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

class MergeOverlappendePeriodeHjelpTest {

    @Test
    void skal_slå_sammen_perioder_riktig_2_godkjent_og_2_underkjent() {
        var iDag = LocalDate.now();
        var akt1 = new OpptjeningAktivitet(iDag.minusMonths(10), iDag, OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt2 = new OpptjeningAktivitet(iDag.minusMonths(6), iDag.plusMonths(4),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt3 = new OpptjeningAktivitet(iDag.minusMonths(4), iDag.minusMonths(2),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        var akt4 = new OpptjeningAktivitet(iDag.plusMonths(5), iDag.plusMonths(6),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);

        var aktiviteter = MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(asList(akt1, akt2, akt3, akt4));

        assertThat(aktiviteter).hasSize(2);
        var godkjent = aktiviteter.get(0);
        var underkjent = aktiviteter.get(1);

        assertThat(godkjent.fom()).isEqualTo(iDag.minusMonths(10));
        assertThat(godkjent.tom()).isEqualTo(iDag.plusMonths(4));
        assertThat(underkjent.fom()).isEqualTo(iDag.plusMonths(5));
        assertThat(underkjent.tom()).isEqualTo(iDag.plusMonths(6));
    }

    @Test
    void skal_slå_sammen_perioder_riktig_3_godkjente_og_1_mellomliggende() {
        var iDag = LocalDate.now();
        var akt1 = new OpptjeningAktivitet(iDag.minusMonths(10), iDag, OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt3 = new OpptjeningAktivitet(iDag.minusMonths(10), iDag.minusMonths(5),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt4 = new OpptjeningAktivitet(iDag.minusMonths(4), iDag, OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt5 = new OpptjeningAktivitet(iDag.minusMonths(5), iDag.minusMonths(4),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE);

        var aktiviteter = MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(asList(akt1, akt3, akt4, akt5));

        assertThat(aktiviteter).hasSize(1);
        var godkjent = aktiviteter.get(0);

        assertThat(godkjent.fom()).isEqualTo(iDag.minusMonths(10));
        assertThat(godkjent.tom()).isEqualTo(iDag);
    }

    @Test
    void skal_håndtere_perioder_som_ikke_henger_sammen() {
        var iDag = LocalDate.now();
        var akt1 = new OpptjeningAktivitet(iDag.minusMonths(3), iDag, OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt3 = new OpptjeningAktivitet(iDag.minusMonths(10), iDag.minusMonths(8),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt4 = new OpptjeningAktivitet(iDag.minusMonths(4), iDag.minusMonths(2),
                OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var aktiviteter = MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(asList(akt1, akt3, akt4));
        assertThat(aktiviteter).hasSize(2);

        var godkjent1 = aktiviteter.get(0);
        var godkjent2 = aktiviteter.get(1);

        assertThat(godkjent1.fom()).isEqualTo(iDag.minusMonths(10));
        assertThat(godkjent1.tom()).isEqualTo(iDag.minusMonths(8));
        assertThat(godkjent2.fom()).isEqualTo(iDag.minusMonths(4));
        assertThat(godkjent2.tom()).isEqualTo(iDag);
    }

    @Test
    void skal_slå_sammen_perioder_riktig_3_godkjente_og_1_mellomliggende_der_mellomliggende_blir_avkortet() {
        var iDag = LocalDate.now();
        var akt1 = new OpptjeningAktivitet(iDag.minusMonths(4), iDag.minusMonths(2), OpptjeningAktivitetType.ARBEID,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt2 = new OpptjeningAktivitet(iDag.minusMonths(2), iDag, OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE,
                OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE);
        var akt3 = new OpptjeningAktivitet(iDag.minusMonths(1), iDag.plusMonths(1), OpptjeningAktivitetType.ARBEIDSAVKLARING,
                OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var aktiviteter = MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(asList(akt1, akt3, akt2));
        assertThat(aktiviteter).hasSize(3);
        var godkjent1 = aktiviteter.get(0);
        var mellom1 = aktiviteter.get(1);
        var godkjent2 = aktiviteter.get(2);

        assertThat(godkjent1.fom()).isEqualTo(iDag.minusMonths(4));
        assertThat(godkjent1.tom()).isEqualTo(iDag.minusMonths(2));
        assertThat(mellom1.fom()).isEqualTo(iDag.minusMonths(2).plusDays(1));
        assertThat(mellom1.tom()).isEqualTo(iDag.minusMonths(1).minusDays(1));
        assertThat(godkjent2.fom()).isEqualTo(iDag.minusMonths(1));
        assertThat(godkjent2.tom()).isEqualTo(iDag.plusMonths(1));
    }

    @Test
    void skal_bevare_bekreftet_avvist_som_ikke_overlapper() {
        var akt1 = new OpptjeningAktivitet(LocalDate.of(2017, 8, 9),
                LocalDate.of(2018, 4, 30), OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        var akt2 = new OpptjeningAktivitet(LocalDate.of(2018, 3, 5),
                LocalDate.of(2018, 3, 21), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt3 = new OpptjeningAktivitet(LocalDate.of(2018, 3, 22),
                LocalDate.of(2018, 4, 6), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt4 = new OpptjeningAktivitet(LocalDate.of(2018, 4, 7),
                LocalDate.of(2018, 6, 8), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt5 = new OpptjeningAktivitet(LocalDate.of(2018, 5, 1),
                LocalDate.of(2018, 6, 8), OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT);

        var aktiviteter = MergeOverlappendePeriodeHjelp
                .mergeOverlappenePerioder(asList(akt1, akt2, akt3, akt4, akt5));
        assertThat(aktiviteter).hasSize(2);
        var underkjent = aktiviteter.get(0);
        var godkjent = aktiviteter.get(1);

        assertThat(underkjent.fom()).isEqualTo(LocalDate.of(2017, 8, 9));
        assertThat(underkjent.tom()).isEqualTo(LocalDate.of(2018, 3, 4));
        assertThat(godkjent.fom()).isEqualTo(LocalDate.of(2018, 3, 5));
        assertThat(godkjent.tom()).isEqualTo(LocalDate.of(2018, 6, 8));
    }

    @Test
    void skal_bevare_bekreftet_avvist_som_ikke_overlapper_når_det_ligger_mellom_godkjent_periode() {
        var akt1 = new OpptjeningAktivitet(LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 2, 1), OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt2 = new OpptjeningAktivitet(LocalDate.of(2018, 4, 1),
                LocalDate.of(2018, 5, 1), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt3 = new OpptjeningAktivitet(LocalDate.of(2018, 7, 1),
                LocalDate.of(2018, 8, 1), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        var akt4 = new OpptjeningAktivitet(LocalDate.of(2017, 12, 1),
                LocalDate.of(2018, 9, 1), OpptjeningAktivitetType.SVANGERSKAPSPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);

        var aktiviteter = MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(asList(akt1, akt2, akt3, akt4));
        assertThat(aktiviteter).hasSize(7);
        var avvist1 = aktiviteter.get(0);
        var godkjent1 = aktiviteter.get(1);
        var avvist2 = aktiviteter.get(2);
        var godkjent2 = aktiviteter.get(3);
        var avvist3 = aktiviteter.get(4);
        var godkjent3 = aktiviteter.get(5);
        var avvist4 = aktiviteter.get(6);

        assertThat(avvist1.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        assertThat(avvist1.fom()).isEqualTo(LocalDate.of(2017, 12, 1));
        assertThat(avvist1.tom()).isEqualTo(LocalDate.of(2017, 12, 31));

        assertThat(godkjent1.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        assertThat(godkjent1.fom()).isEqualTo(LocalDate.of(2018, 1, 1));
        assertThat(godkjent1.tom()).isEqualTo(LocalDate.of(2018, 2, 1));

        assertThat(avvist2.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        assertThat(avvist2.fom()).isEqualTo(LocalDate.of(2018, 2, 2));
        assertThat(avvist2.tom()).isEqualTo(LocalDate.of(2018, 3, 31));

        assertThat(godkjent2.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        assertThat(godkjent2.fom()).isEqualTo(LocalDate.of(2018, 4, 1));
        assertThat(godkjent2.tom()).isEqualTo(LocalDate.of(2018, 5, 1));

        assertThat(avvist3.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        assertThat(avvist3.fom()).isEqualTo(LocalDate.of(2018, 5, 2));
        assertThat(avvist3.tom()).isEqualTo(LocalDate.of(2018, 6, 30));

        assertThat(godkjent3.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        assertThat(godkjent3.fom()).isEqualTo(LocalDate.of(2018, 7, 1));
        assertThat(godkjent3.tom()).isEqualTo(LocalDate.of(2018, 8, 1));

        assertThat(avvist4.klasse()).isEqualTo(OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);
        assertThat(avvist4.fom()).isEqualTo(LocalDate.of(2018, 8, 2));
        assertThat(avvist4.tom()).isEqualTo(LocalDate.of(2018, 9, 1));
    }
}
