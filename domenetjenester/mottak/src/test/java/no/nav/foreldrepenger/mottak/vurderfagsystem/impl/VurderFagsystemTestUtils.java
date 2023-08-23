package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class VurderFagsystemTestUtils {

    public static final JournalpostId JOURNALPOST_ID = new JournalpostId("1");
    public static final Long ÅPEN_FAGSAK_ID_1 = 1L;
    public static final Long ÅPEN_FAGSAK_ID_2 = 2L;
    public static final Saksnummer ÅPEN_SAKSNUMMER_1 = new Saksnummer(ÅPEN_FAGSAK_ID_1 * 2 + "");
    public static final Saksnummer ÅPEN_SAKSNUMMER_2 = new Saksnummer(ÅPEN_FAGSAK_ID_2 * 2 + "");
    public static final Long AVSLT_NY_FAGSAK_ID_1 = 11L;
    public static final Long AVSLT_NY_FAGSAK_ID_2 = 2L;
    public static final Long AVSLT_GAMMEL_FAGSAK_ID_1 = 111L;
    public static final AktørId ANNEN_PART_ID = AktørId.dummy();
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    public static final LocalDate BARN_TERMINDATO = LocalDate.of(2019, 02, 03);
    public static final LocalDate BARN_FØDSELSDATO = LocalDate.of(2019, 02, 04);
    public static final String ARBEIDSFORHOLDSID = "arbeidsforholdsId123";
    public static final String VIRKSOMHETSNUMMER = "123456789";

    public static Optional<Behandling> byggBehandlingMedEndretDato(Fagsak fagsak, int dagerSidenSisteBehandling) {

        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak)
                .medOpprettetDato(LocalDateTime.now().minusDays(dagerSidenSisteBehandling));
        var behandling = behandlingBuilder.build();
        return Optional.of(behandling);
    }

    public static VurderFagsystem byggVurderFagsystemMedTermin(LocalDate terminDatdato, BehandlingTema behandlingTema, boolean erStrukturertSøknad) {
        var vfData = new VurderFagsystem();
        vfData.setBehandlingTema(behandlingTema);
        vfData.setAktørId(BRUKER_AKTØR_ID);
        vfData.setStrukturertSøknad(erStrukturertSøknad);
        vfData.setJournalpostId(JOURNALPOST_ID);
        vfData.setBarnTermindato(terminDatdato);
        return vfData;
    }

    public static Fagsak fagsakFødselMedId(Long forventetFagsakId) {

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, BehandlingslagerTestUtil.lagNavBruker());
        fagsak.setId(forventetFagsakId);
        return fagsak;
    }

    public static VurderFagsystem byggVurderFagsystem(BehandlingTema behandlingTema, boolean erStrukturertSøknad) {
        var vfData = new VurderFagsystem();
        vfData.setBehandlingTema(behandlingTema);
        vfData.setAktørId(BRUKER_AKTØR_ID);
        vfData.setStrukturertSøknad(erStrukturertSøknad);
        vfData.setJournalpostId(JOURNALPOST_ID);
        return vfData;
    }

    public static VurderFagsystem byggVurderFagsystemForInntektsmelding(String årsakInnsending, BehandlingTema behandlingTema,
            LocalDateTime forsendelseMottatt,
            AktørId aktørId, JournalpostId journalpostId, String arbeidsforholdsid, String setArbeidsgiverIdentifikator) {
        var fagsystem = new VurderFagsystem();
        fagsystem.setAktørId(aktørId);
        fagsystem.setJournalpostId(journalpostId);
        fagsystem.setBehandlingTema(behandlingTema);
        fagsystem.setÅrsakInnsendingInntektsmelding(årsakInnsending);
        fagsystem.setForsendelseMottattTidspunkt(forsendelseMottatt);
        fagsystem.setArbeidsforholdsid(arbeidsforholdsid);
        fagsystem.setVirksomhetsnummer(setArbeidsgiverIdentifikator);
        return fagsystem;
    }

    public static Behandling byggBehandlingUdefinert(Fagsak fagsak) {
        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        return behandlingBuilder.build();
    }

    public static VurderFagsystem byggVurderFagsystemMedAnnenPart(BehandlingTema behandlingTema, AktørId annenPartId, Saksnummer saksnr,
            AktørId aktørId,
            JournalpostId journalpostId, LocalDate barnTermindato, LocalDate barnFødselsdato) {
        var fagsystem = new VurderFagsystem();
        fagsystem.setAnnenPart(annenPartId);
        fagsystem.setSaksnummer(saksnr);
        fagsystem.setStrukturertSøknad(true);
        fagsystem.setAktørId(aktørId);
        fagsystem.setJournalpostId(journalpostId);
        fagsystem.setBehandlingTema(behandlingTema);
        fagsystem.setBarnTermindato(barnTermindato);
        fagsystem.setBarnFodselsdato(barnFødselsdato);
        return fagsystem;
    }

    public static Fagsak buildFagsakMedUdefinertRelasjon(Long fagsakid, boolean erAvsluttet) {
        var navBruker = BehandlingslagerTestUtil.lagNavBruker();
        var fagsak = FagsakBuilder.nyForeldrepengesak(RelasjonsRolleType.MORA)
                .medBruker(navBruker)
                .medSaksnummer(new Saksnummer(fagsakid + ""))
                .build();
        fagsak.setId(fagsakid);
        fagsak.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        if (erAvsluttet) {
            fagsak.setAvsluttet();
        }
        return fagsak;
    }
}
