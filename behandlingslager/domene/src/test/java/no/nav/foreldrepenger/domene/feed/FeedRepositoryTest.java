package no.nav.foreldrepenger.domene.feed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

//Trengs en omskriving hvis testene skal kunne kjøres i parallell
class FeedRepositoryTest extends EntityManagerAwareTest {

    private static final String TYPE2 = "type2";
    private static final String TYPE1 = "type1";
    private static final String AKTØR_ID = "1000000000";
    private static final String AKTØR_ID_2 = "1000000001";
    private static final String KILDE_ID = "kildeId";
    private static final String PAYLOAD = "{\"hello\": \"world\"}";

    private FeedRepository feedRepository;
    private FpVedtakUtgåendeHendelse hendelseAvType1MedAktørId1MedSek1;
    private FpVedtakUtgåendeHendelse hendelseAvType1MedAktørId2MedSek2;
    private FpVedtakUtgåendeHendelse hendelseAvType2MedAktørId1MedSek3;
    private FpVedtakUtgåendeHendelse hendelseMedHøyestSeksvensnummerogKilde;

    @BeforeEach
    void setUp() {
        feedRepository = new FeedRepository(getEntityManager());
    }

    private void lagreHendelser() {
        if (hendelseAvType1MedAktørId1MedSek1 == null) {
            hendelseAvType1MedAktørId1MedSek1 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID).type(TYPE1).build();
            var nesteSekvensnummer = nesteSeksvensnummer();
            hendelseAvType1MedAktørId1MedSek1.setSekvensnummer(nesteSekvensnummer);
            feedRepository.lagre(hendelseAvType1MedAktørId1MedSek1);
        }

        if (hendelseAvType1MedAktørId2MedSek2 == null) {
            hendelseAvType1MedAktørId2MedSek2 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID_2).type(TYPE1).build();
            hendelseAvType1MedAktørId2MedSek2.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseAvType1MedAktørId2MedSek2);
        }

        if (hendelseAvType2MedAktørId1MedSek3 == null) {
            hendelseAvType2MedAktørId1MedSek3 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID).type(TYPE2).build();
            hendelseAvType2MedAktørId1MedSek3.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseAvType2MedAktørId1MedSek3);
        }

        if (hendelseMedHøyestSeksvensnummerogKilde == null) {
            hendelseMedHøyestSeksvensnummerogKilde = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId("1000000002")
                .type("type3").kildeId(KILDE_ID).build();
            hendelseMedHøyestSeksvensnummerogKilde.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseMedHøyestSeksvensnummerogKilde);
        }
    }

    private long nesteSeksvensnummer() {
        return feedRepository.hentNesteSekvensnummer(FpVedtakUtgåendeHendelse.class);
    }

    @Test
    void skal_lagre_fp_hendelse() {
        lagreHendelser();
        var utgåendeHendelse = byggUtgåendeFpHendelse();
        long id = feedRepository.lagre(utgåendeHendelse);
        assertThat(id).isPositive();
        var utgåendeHendelse1 = feedRepository.hentUtgåendeHendelse(id);

        assertThat(utgåendeHendelse1.get()).isNotNull();
        assertThat(utgåendeHendelse1.get().getId()).isEqualTo(id);
        assertThat(utgåendeHendelse1.get().getSekvensnummer()).isPositive();
    }

    @Test
    void skal_lagre_svp_hendelse() {
        lagreHendelser();
        var utgåendeHendelse = byggUtgåendeSvpHendelse();
        long id = feedRepository.lagre(utgåendeHendelse);
        assertThat(id).isPositive();
        var utgåendeHendelse1 = feedRepository.hentUtgåendeHendelse(id);

        assertThat(utgåendeHendelse1.get()).isNotNull();
        assertThat(utgåendeHendelse1.get().getId()).isEqualTo(id);
        assertThat(utgåendeHendelse1.get().getSekvensnummer()).isPositive();
    }

    @Test
    void skal_returnere_true_hvis_hendelse_med_kilde_id_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId(KILDE_ID)).isTrue();
    }

    @Test
    void skal_returnere_false_hvis_hendelse_med_kilde_id_ikke_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId("eksisterer_ikke")).isFalse();
    }

    @Test
    void skal_lagre_hendelse_flushe_sjekke_om_kilde_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId("ny_kilde")).isFalse();
        var utgåendeHendelse = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD)
            .aktørId("1000000002")
            .type("type3")
            .kildeId("ny_kilde")
            .build();
        feedRepository.lagre(utgåendeHendelse);
        assertThat(feedRepository.harHendelseMedKildeId("ny_kilde")).isTrue();
    }

    @Test
    void skal_hente_hendelser_med_type1() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder()
                    .medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medType(TYPE1)
                    .medMaxAntall(100L).build());

        assertThat(alle).contains(hendelseAvType1MedAktørId1MedSek1, hendelseAvType1MedAktørId2MedSek2);
    }

    @Test
    void skal_hente_alle_hendelser_med_sekvens_id_større_enn_sist_lest() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId2MedSek2.getSekvensnummer() - 1)
                    .medMaxAntall(100L).build());

        assertThat(alle).contains(hendelseAvType1MedAktørId2MedSek2, hendelseAvType2MedAktørId1MedSek3, hendelseMedHøyestSeksvensnummerogKilde);
    }

    @Test
    void skal_returnerer_tom_liste_hvis_result_set_er_tom() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(nesteSeksvensnummer() + 100).medMaxAntall(100L).build());

        assertThat(alle).isEmpty();
    }

    @Test
    void skal_hente_alle_hendelser_med_aktør_id() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(0L).medAktørId(AKTØR_ID_2).medMaxAntall(100L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId2MedSek2);
    }

    @Test
    void skal_hente_max_antall_1() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medMaxAntall(1L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId1MedSek1);
    }

    @Test
    void skal_hente_max_antall_4_med_hopp_i_sekvensnummer() {
        lagreHendelser();
        var alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medMaxAntall(4L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId1MedSek1, hendelseAvType1MedAktørId2MedSek2,
                hendelseAvType2MedAktørId1MedSek3, hendelseMedHøyestSeksvensnummerogKilde);
    }

    private static FpVedtakUtgåendeHendelse byggUtgåendeFpHendelse() {
        return FpVedtakUtgåendeHendelse.builder()
                .payload(PAYLOAD)
                .aktørId(AKTØR_ID)
                .type(TYPE1)
                .build();
    }

    private static SvpVedtakUtgåendeHendelse byggUtgåendeSvpHendelse() {
        return SvpVedtakUtgåendeHendelse.builder()
                .payload(PAYLOAD)
                .aktørId(AKTØR_ID)
                .type(TYPE1)
                .build();
    }
}
