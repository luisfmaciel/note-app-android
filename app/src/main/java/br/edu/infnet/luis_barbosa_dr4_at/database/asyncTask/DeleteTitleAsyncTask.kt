package br.edu.infnet.luis_barbosa_dr4_at.database.asyncTask

import android.os.AsyncTask
import br.edu.infnet.luis_barbosa_dr4_at.Util
import br.edu.infnet.luis_barbosa_dr4_at.database.dao.NoteDao
import br.edu.infnet.luis_barbosa_dr4_at.model.Note

class DeleteTitleAsyncTask (
    private val mAsyncTaskDao: NoteDao,
) : AsyncTask<String, Unit, Unit >()
{
    override fun doInBackground(vararg params: String?) {
        mAsyncTaskDao.deleteNoteByTitle(params[0]!!)

    }
}