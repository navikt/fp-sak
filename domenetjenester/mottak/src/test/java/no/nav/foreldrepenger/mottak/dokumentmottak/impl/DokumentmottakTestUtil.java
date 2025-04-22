package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class DokumentmottakTestUtil {

    static MottattDokument byggMottattDokument(DokumentTypeId dokumentTypeId, Long fagsakId, String xml, LocalDate mottattDato, boolean elektroniskRegistrert,
                                               String journalpostId) {
        var builder = new MottattDokument.Builder();
        builder.medDokumentType(dokumentTypeId);
        builder.medMottattDato(mottattDato);
        builder.medXmlPayload(xml);
        builder.medElektroniskRegistrert(elektroniskRegistrert);
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        return builder.build();
    }

    static MottattDokument byggMottattPapirsøknad(DokumentTypeId dokumentTypeId, Long fagsakId, String xml, LocalDate mottattDato,
                                                  boolean elektroniskRegistrert, String journalpostId) {
        var builder = new MottattDokument.Builder();
        builder.medDokumentType(dokumentTypeId);
        builder.medDokumentKategori(DokumentKategori.SØKNAD);
        builder.medMottattDato(mottattDato);
        builder.medXmlPayload(xml);
        builder.medElektroniskRegistrert(elektroniskRegistrert);
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        return builder.build();
    }

    static Fagsak byggFagsak(AktørId aktørId, RelasjonsRolleType rolle, NavBrukerKjønn kjønn, Saksnummer saksnummer,
                             FagsakRepository fagsakRepository) {
        var navBruker = new NavBrukerBuilder()
            .medAktørId(aktørId)
            .medKjønn(kjønn)
            .build();
        var fagsak = FagsakBuilder.nyForeldrepengesak(rolle)
            .medSaksnummer(saksnummer)
            .medBruker(navBruker).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    public static BehandlingVedtak oppdaterVedtaksresultat(Behandling origBehandling, VedtakResultatType vedtakResultatType) {

        return BehandlingVedtak.builder()
            .medVedtakResultatType(vedtakResultatType)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(origBehandling.getBehandlingsresultat())
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();
    }
}
