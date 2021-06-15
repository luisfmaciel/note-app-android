package br.edu.infnet.luis_barbosa_dr4_at.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.edu.infnet.luis_barbosa_dr4_at.database.repository.NotesRepository
import br.edu.infnet.luis_barbosa_dr4_at.model.Note

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    val getAllNotes = MutableLiveData<Array<Note>>()


    private var mRepository: NotesRepository = NotesRepository(application)

    private var mAllNotes: LiveData<List<Note>> = mRepository.getAllNotes()

    fun getAllNotes(): LiveData<List<Note>> {
        return mAllNotes
    }

    fun insert(note: Note){
        mRepository.insert(note)
    }

    fun deleteAll(){
        mRepository.deleteAll()
    }

    fun deleteNote(note: Note){
        mRepository.deleteNote(note)
    }

    fun updateNote(note: Note){
        mRepository.updateNote(note)
    }
}